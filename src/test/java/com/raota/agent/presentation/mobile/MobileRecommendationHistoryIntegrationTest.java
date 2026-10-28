package com.raota.agent.presentation.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.agent.domain.recommendation.model.MobileRecommendationRun;
import com.raota.agent.domain.recommendation.repository.MobileRecommendationRunRepository;
import com.raota.global.presentation.common.RequestIdFilter;
import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.model.Nickname;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import com.raota.mobile.account.infrastructure.auth.MobileAccessTokenService;
import com.raota.mobile.common.cursor.Cursor;
import com.raota.support.BaseIntegrationTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** 앱의 "지난 식권" 목록이다. 오늘 것을 포함해 매장을 보여준 추천을 최신순 커서로 읽는다. */
class MobileRecommendationHistoryIntegrationTest extends BaseIntegrationTest {

    private static final long OPEN_SHOP = 900004801L;

    private static final long OTHER_OPEN_SHOP = 900004802L;

    private static final long CLOSED_SHOP = 900004803L;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private RequestIdFilter requestIdFilter;

    @Autowired
    private MobileUserRepository users;

    @Autowired
    private MobileAccessTokenService tokens;

    @Autowired
    private MobileRecommendationRunRepository runs;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper mapper;

    private MockMvc mvc;

    private final List<Long> created = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(requestIdFilter)
                .apply(springSecurity())
                .build();
        insertShop(OPEN_SHOP, "OPERATIONAL");
        insertShop(OTHER_OPEN_SHOP, "OPERATIONAL");
        insertShop(CLOSED_SHOP, "CLOSED");
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM tb_v2_shop WHERE id IN (?, ?, ?)", OPEN_SHOP, OTHER_OPEN_SHOP, CLOSED_SHOP);
        created.forEach(runs::deleteByUserId);
        created.forEach(users::deleteById);
    }

    @Test
    void 최신순이고_뽑은_시각이_같으면_ID가_큰_식권이_먼저이며_페이지_경계에서_빠지거나_겹치지_않는다() throws Exception {
        Long member = active();
        Instant older = Instant.parse("2026-10-20T03:00:00Z");
        Instant sameTime = Instant.parse("2026-10-25T03:00:00.123456Z");
        Long oldest = save(member, older, OPEN_SHOP);
        Long first = save(member, sameTime, OPEN_SHOP);
        Long second = save(member, sameTime, OPEN_SHOP);
        Long third = save(member, sameTime, OPEN_SHOP);

        JsonNode firstPage = data(history(member).param("size", "2"));
        assertThat(ids(firstPage)).containsExactly(third, second);
        assertThat(firstPage.get("hasNext").asBoolean()).isTrue();

        JsonNode secondPage = data(history(member)
                .param("size", "2")
                .param("cursor", firstPage.get("nextCursor").asString()));
        assertThat(ids(secondPage)).containsExactly(first, oldest);
        assertThat(secondPage.get("hasNext").asBoolean()).isFalse();
        assertThat(secondPage.get("nextCursor").isNull()).isTrue();
    }

    @Test
    void 다른_회원의_식권과_매장을_보여주지_못한_추천은_나오지_않는다() throws Exception {
        Long member = active();
        Long other = active();
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        Long shown = save(member, now.minus(2, ChronoUnit.DAYS), OPEN_SHOP);
        save(member, now.minus(1, ChronoUnit.DAYS));
        save(other, now, OPEN_SHOP);

        assertThat(ids(data(history(member)))).containsExactly(shown);
    }

    @Test
    void 지금은_추천하지_않는_매장만_있던_식권도_빈_매장_목록으로_남기고_다른_식권은_보여준_순서를_지킨다() throws Exception {
        Long member = active();
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        Long closedOnly = save(member, now.minus(3, ChronoUnit.DAYS), CLOSED_SHOP);
        Long mixed = save(member, now.minus(2, ChronoUnit.DAYS), OTHER_OPEN_SHOP, CLOSED_SHOP, OPEN_SHOP);

        JsonNode items = data(history(member)).get("items");

        assertThat(items.get(0).get("recommendationId").asString()).isEqualTo(String.valueOf(mixed));
        assertThat(shopIds(items.get(0))).containsExactly(String.valueOf(OTHER_OPEN_SHOP), String.valueOf(OPEN_SHOP));
        assertThat(items.get(1).get("recommendationId").asString()).isEqualTo(String.valueOf(closedOnly));
        assertThat(items.get(1).get("shops").isEmpty()).isTrue();
        assertThat(items.get(1).get("appliedConditions").toString()).isEqualTo("[\"쇼유\"]");
        assertThat(items.get(1).get("reasons").toString())
                .isEqualTo("{\"" + CLOSED_SHOP + "\":\"" + CLOSED_SHOP + " 이유\"}");
    }

    @Test
    void 오늘_식권도_포함하고_오늘_API와_같은_내용이다() throws Exception {
        Long member = active();
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        save(member, now.minus(3, ChronoUnit.DAYS), OPEN_SHOP);
        Long today = save(member, now, OTHER_OPEN_SHOP, CLOSED_SHOP, OPEN_SHOP);

        JsonNode history = data(history(member)).get("items");
        JsonNode todayTickets = data(get("/api/v2/ai/recommendations/today")
                        .header(HttpHeaders.AUTHORIZATION, auth(member)))
                .get("tickets");

        assertThat(history.size()).isEqualTo(2);
        assertThat(history.get(0).get("recommendationId").asString()).isEqualTo(String.valueOf(today));
        assertThat(history.get(0)).isEqualTo(todayTickets.get(0));
    }

    @Test
    void 탈퇴로_추천_기록을_지우면_빈_목록이다() throws Exception {
        Long member = active();
        save(member, Instant.now().truncatedTo(ChronoUnit.MICROS), OPEN_SHOP);

        runs.deleteByUserId(member);

        JsonNode page = data(history(member));
        assertThat(page.get("items").isEmpty()).isTrue();
        assertThat(page.get("hasNext").asBoolean()).isFalse();
    }

    @Test
    void 목록은_로그인과_크기_및_커서_형식을_검증한다() throws Exception {
        mvc.perform(get("/api/v2/ai/recommendations")).andExpect(status().isUnauthorized());
        Long member = active();
        for (String size : List.of("0", "51")) {
            mvc.perform(history(member).param("size", size)).andExpect(status().isBadRequest());
        }
        // 형식은 맞지만 정렬값이 시각이 아닌 커서도 거절한다
        for (String cursor : List.of("not-a-cursor", Cursor.of(123L, 5L).encode())) {
            mvc.perform(history(member).param("cursor", cursor)).andExpect(status().isBadRequest());
        }
    }

    /** 이 순서대로 매장을 보여준 추천을 남긴다. 매장이 없으면 아무것도 보여주지 못한 추천이다. */
    private Long save(Long userId, Instant createdAt, long... shopIds) {
        List<MobileRecommendationRun.Item> items = new ArrayList<>();
        for (int index = 0; index < shopIds.length; index++) {
            items.add(new MobileRecommendationRun.Item(index + 1, shopIds[index], 0.5, shopIds[index] + " 이유"));
        }
        return runs.save(new MobileRecommendationRun(
                userId, "{\"soup\":\"쇼유\"}", null, null, 10, "test", "test", "test", 100, createdAt, items));
    }

    private MockHttpServletRequestBuilder history(Long userId) {
        return get("/api/v2/ai/recommendations").header(HttpHeaders.AUTHORIZATION, auth(userId));
    }

    private JsonNode data(MockHttpServletRequestBuilder request) throws Exception {
        byte[] body = mvc.perform(request)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        return mapper.readTree(body).get("data");
    }

    private static List<Long> ids(JsonNode page) {
        List<Long> ids = new ArrayList<>();
        page.get("items")
                .forEach(item ->
                        ids.add(Long.valueOf(item.get("recommendationId").asString())));
        return ids;
    }

    private static List<String> shopIds(JsonNode ticket) {
        List<String> ids = new ArrayList<>();
        ticket.get("shops").forEach(shop -> ids.add(shop.get("id").asString()));
        return ids;
    }

    private void insertShop(long id, String businessStatus) {
        jdbc.update("""
                INSERT INTO tb_v2_shop (id, name, branch_name, address, region, latitude, longitude, ramen_types, tags,
                    business_status, ai_summary_keywords, is_published, created_at, updated_at, deleted_at)
                VALUES (?, '지난 식권 매장', '본점', '서울 마포구 와우산로 1', '마포구', 37.55630000, 126.92360000, '["쇼유"]', '[]',
                    ?, '[]', true, '2026-09-01 00:00:00', '2026-09-01 00:00:00', NULL)
                """, id, businessStatus);
    }

    private Long active() {
        MobileUser user = MobileUser.onboarding("history-" + UUID.randomUUID() + "@example.com");
        user.completeOnboarding(Nickname.of("H" + UUID.randomUUID().toString().substring(0, 8)), Instant.now());
        Long id = users.saveAndFlush(user).getId();
        created.add(id);
        return id;
    }

    private String auth(Long userId) {
        return "Bearer " + tokens.issue(userId);
    }
}
