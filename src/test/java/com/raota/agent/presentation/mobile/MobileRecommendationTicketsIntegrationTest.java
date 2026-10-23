package com.raota.agent.presentation.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.agent.application.mobile.port.MobileRecommendationReasonPort;
import com.raota.agent.domain.recommendation.repository.MobileRecommendationRunRepository;
import com.raota.global.presentation.common.RequestIdFilter;
import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.model.Nickname;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import com.raota.mobile.account.infrastructure.auth.MobileAccessTokenService;
import com.raota.support.BaseIntegrationTest;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** 앱 AI 큐레이터의 하루 식권(기본 3장)과 "오늘 뽑은 식권 다시 보기"다. */
class MobileRecommendationTicketsIntegrationTest extends BaseIntegrationTest {

    private static final long SHOYU = 900004701L;

    private static final long SHIO = 900004702L;

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

    @MockitoBean
    private MobileRecommendationReasonPort reasonWriter;

    private MockMvc mvc;

    private final List<Long> created = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(requestIdFilter).apply(springSecurity()).build();
        insertShop(SHOYU, "[\"쇼유\"]");
        insertShop(SHIO, "[\"시오\"]");
        List<Document> documents = List.of(Document.builder()
            .text("매장 " + SHOYU)
            .metadata(Map.of("documentType", "SHOP_PROFILE", "shopId", String.valueOf(SHOYU)))
            .score(0.9)
            .build());
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(documents);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM tb_v2_shop WHERE id IN (?, ?)", SHOYU, SHIO);
        created.forEach(runs::deleteByUserId);
        created.forEach(users::deleteById);
    }

    @Test
    void 매장을_보여준_추천만_하루_3장으로_세고_4장째는_429_DAILY_LIMIT_EXCEEDED다() throws Exception {
        Long member = active();

        // 고른 국물의 매장이 없어 아무것도 보여주지 못한 추천은 식권을 쓰지 않는다
        recommend(member, "{\"soup\":\"지로\"}").andExpect(status().isOk())
            .andExpect(jsonPath("$.data.shops.length()").value(0));
        assertThat(remaining(member)).isEqualTo(3);
        for (int remaining = 2; remaining >= 0; remaining--) {
            recommend(member, "{\"soup\":\"쇼유\",\"mood\":\"혼밥\"}").andExpect(status().isOk());
            assertThat(remaining(member)).isEqualTo(remaining);
        }

        recommend(member, "{\"soup\":\"쇼유\"}").andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.error.code").value("DAILY_LIMIT_EXCEEDED"));
        // 다른 회원의 식권은 따로 센다
        Long other = active();
        recommend(other, "{\"soup\":\"쇼유\"}").andExpect(status().isOk());
        assertThat(remaining(other)).isEqualTo(2);
    }

    @Test
    void 어제_뽑은_식권은_오늘_장수에_들지_않는다() throws Exception {
        Long member = active();
        for (int index = 0; index < 3; index++) {
            recommend(member, "{\"soup\":\"쇼유\"}").andExpect(status().isOk());
        }
        jdbc.update("UPDATE tb_v2_recommendation_run SET created_at = ? WHERE user_id = ?",
                Timestamp.from(Instant.now().minus(2, ChronoUnit.DAYS)), member);

        recommend(member, "{\"soup\":\"쇼유\"}").andExpect(status().isOk());
        assertThat(remaining(member)).isEqualTo(2);
    }

    @Test
    void 오늘_뽑은_식권을_최신순으로_그때의_매장_조건_추천_이유와_함께_다시_보여준다() throws Exception {
        Long member = active();
        when(reasonWriter.reasons(any(), any())).thenReturn(Map.of(String.valueOf(SHOYU), "맑은 쇼유 국물이 혼밥에 잘 맞아요."));
        String first = data(recommend(member, "{\"soup\":\"쇼유\",\"mood\":\"혼밥\"}").andExpect(status().isOk()))
            .get("recommendationId")
            .asString();
        when(reasonWriter.reasons(any(), any())).thenReturn(Map.of());
        String second = data(
                recommend(member, "{\"soup\":\"시오\",\"region\":\"마포구\",\"latitude\":37.5563,\"longitude\":126.9236}")
                    .andExpect(status().isOk()))
            .get("recommendationId")
            .asString();
        recommend(active(), "{\"soup\":\"쇼유\"}").andExpect(status().isOk());

        JsonNode today = data(
                mvc.perform(get("/api/v2/ai/recommendations/today").header(HttpHeaders.AUTHORIZATION, auth(member)))
                    .andExpect(status().isOk()));

        assertThat(today.get("dailyLimit").asInt()).isEqualTo(3);
        assertThat(today.get("remaining").asInt()).isEqualTo(1);
        JsonNode tickets = today.get("tickets");
        assertThat(tickets.size()).isEqualTo(2);
        assertThat(tickets.get(0).get("recommendationId").asString()).isEqualTo(second);
        assertThat(tickets.get(0).get("shops").get(0).get("id").asString()).isEqualTo(String.valueOf(SHIO));
        assertThat(tickets.get(0).get("appliedConditions").toString()).isEqualTo("[\"시오\",\"마포구\",\"가까운 순\"]");
        assertThat(tickets.get(0).get("reasons").isEmpty()).isTrue();
        assertThat(tickets.get(1).get("recommendationId").asString()).isEqualTo(first);
        assertThat(tickets.get(1).get("appliedConditions").toString()).isEqualTo("[\"쇼유\",\"혼밥\"]");
        assertThat(tickets.get(1).get("reasons").toString()).isEqualTo("{\"" + SHOYU + "\":\"맑은 쇼유 국물이 혼밥에 잘 맞아요.\"}");
    }

    @Test
    void 오늘_식권_조회는_로그인한_회원만_쓴다() throws Exception {
        mvc.perform(get("/api/v2/ai/recommendations/today")).andExpect(status().isUnauthorized());
    }

    private ResultActions recommend(Long userId, String body) throws Exception {
        return mvc.perform(post("/api/v2/ai/recommendations").header(HttpHeaders.AUTHORIZATION, auth(userId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
    }

    private int remaining(Long userId) throws Exception {
        return data(
                mvc.perform(get("/api/v2/ai/recommendations/today").header(HttpHeaders.AUTHORIZATION, auth(userId))))
            .get("remaining")
            .asInt();
    }

    private JsonNode data(ResultActions result) throws Exception {
        return mapper.readTree(result.andReturn().getResponse().getContentAsByteArray()).get("data");
    }

    private void insertShop(long id, String ramenTypes) {
        jdbc.update("""
                INSERT INTO tb_v2_shop (id, name, branch_name, address, region, latitude, longitude, ramen_types, tags,
                    business_status, ai_summary_keywords, is_published, created_at, updated_at, deleted_at)
                VALUES (?, '식권 매장', '본점', '서울 마포구 와우산로 1', '마포구', 37.55630000, 126.92360000, ?, '[]',
                    'OPERATIONAL', '[]', true, '2026-09-01 00:00:00', '2026-09-01 00:00:00', NULL)
                """, id, ramenTypes);
    }

    private Long active() {
        MobileUser user = MobileUser.onboarding("ticket-" + UUID.randomUUID() + "@example.com");
        user.completeOnboarding(Nickname.of("T" + UUID.randomUUID().toString().substring(0, 8)), Instant.now());
        Long id = users.saveAndFlush(user).getId();
        created.add(id);
        return id;
    }

    private String auth(Long userId) {
        return "Bearer " + tokens.issue(userId);
    }

}
