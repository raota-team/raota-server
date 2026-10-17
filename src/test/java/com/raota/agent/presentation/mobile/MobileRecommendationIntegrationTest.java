package com.raota.agent.presentation.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.agent.domain.recommendation.repository.MobileRecommendationRunRepository;
import com.raota.global.presentation.common.RequestIdFilter;
import com.raota.mobile.account.application.service.MobileWithdrawalPurgeService;
import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.model.Nickname;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import com.raota.mobile.account.infrastructure.auth.MobileAccessTokenService;
import com.raota.support.BaseIntegrationTest;
import java.time.Duration;
import java.time.Instant;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@TestPropertySource(properties = "app.mobile.recommendation.timeout=500ms")
class MobileRecommendationIntegrationTest extends BaseIntegrationTest {

    private static final long NEAR = 900004601L;

    private static final long HIDDEN = 900004602L;

    private static final long DELETED = 900004603L;

    private static final long CLOSED = 900004604L;

    private static final long FAR = 900004605L;

    private static final long NO_COORDINATES = 900004606L;

    private static final String NEAR_POINT = "\"latitude\":37.5563,\"longitude\":126.9236";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private RequestIdFilter requestIdFilter;

    @Autowired
    private MobileUserRepository users;

    @Autowired
    private MobileAccessTokenService tokens;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private MobileWithdrawalPurgeService purges;

    @MockitoSpyBean
    private MobileRecommendationRunRepository runs;

    private MockMvc mvc;

    private final List<Long> created = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(requestIdFilter).apply(springSecurity()).build();
        insertShop(NEAR, "마포구", "서울 마포구 와우산로 1", "37.55630000", "126.92360000", "OPERATIONAL", true, false);
        insertShop(HIDDEN, "마포구", "서울 마포구 와우산로 2", "37.55630000", "126.92360000", "UNKNOWN", false, false);
        insertShop(DELETED, "마포구", "서울 마포구 와우산로 3", "37.55630000", "126.92360000", "OPERATIONAL", true, true);
        insertShop(CLOSED, "마포구", "서울 마포구 와우산로 4", "37.55630000", "126.92360000", "CLOSED", true, false);
        insertShop(FAR, "강남구", "서울 강남구 테헤란로 1", "37.49790000", "127.02760000", "OPERATIONAL", true, false);
        insertShop(NO_COORDINATES, "망원동", "서울 마포구 망원로 1", null, null, "TEMPORARILY_CLOSED", true, false);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM tb_v2_shop_bookmark WHERE shop_id BETWEEN ? AND ?", NEAR, NO_COORDINATES);
        jdbc.update("DELETE FROM tb_v2_shop WHERE id BETWEEN ? AND ?", NEAR, NO_COORDINATES);
        for (Long userId : created) {
            runs.deleteByUserId(userId);
        }
        created.forEach(users::deleteById);
    }

    @Test
    void 공개되고_영업하는_v2_매장만_순위대로_한_번씩_돌려주고_v2_북마크와_상세로_이어진다() throws Exception {
        Long member = active();
        jdbc.update(
                "INSERT INTO tb_v2_shop_bookmark (user_id, shop_id, created_at) VALUES (?, ?, CURRENT_TIMESTAMP(6))",
                member, FAR);
        searchReturns(FAR, NEAR, HIDDEN, DELETED, CLOSED, NO_COORDINATES, NEAR);

        JsonNode data = data(recommend(member, "{\"soup\":\"진한 돈코츠\",\"mood\":\"혼밥\",\"text\":\"차슈 많은 곳\"}")
            .andExpect(status().isOk()));

        assertThat(ids(data)).containsExactly(String.valueOf(FAR), String.valueOf(NEAR));
        assertThat(data.get("shops").get(0).get("isBookmarked").asBoolean()).isTrue();
        assertThat(data.get("shops").get(1).get("isBookmarked").asBoolean()).isFalse();
        assertThat(data.get("shops").get(1).get("distanceMeters").isNull()).isTrue();
        assertThat(data.get("appliedConditions").toString()).isEqualTo("[\"진한 돈코츠\",\"혼밥\"]");
        mvc.perform(get("/api/v2/shops/{shopId}", data.get("shops").get(0).get("id").asString()))
            .andExpect(status().isOk());
    }

    @Test
    void 지역이나_좌표가_오면_그_안의_매장만_남기고_조건에_표시한다() throws Exception {
        Long member = active();
        searchReturns(FAR, NEAR, NO_COORDINATES);

        JsonNode byRegion = data(recommend(member, "{\"soup\":\"쇼유\",\"region\":\"마포구\"}").andExpect(status().isOk()));
        assertThat(ids(byRegion)).containsExactly(String.valueOf(NEAR));
        assertThat(byRegion.get("appliedConditions").toString()).isEqualTo("[\"쇼유\",\"마포구\"]");

        JsonNode byPoint = data(recommend(member, "{\"soup\":\"쇼유\"," + NEAR_POINT + "}").andExpect(status().isOk()));
        assertThat(ids(byPoint)).containsExactly(String.valueOf(NEAR));
        assertThat(byPoint.get("shops").get(0).get("distanceMeters").asInt()).isZero();
        assertThat(byPoint.get("appliedConditions").toString()).isEqualTo("[\"쇼유\",\"3km 이내\"]");
    }

    @Test
    void 보여준_매장과_조건을_노출_기록으로_남기고_기록_id를_응답한다() throws Exception {
        Long member = active();
        searchReturns(FAR, NEAR, HIDDEN);

        JsonNode data = data(recommend(member,
                "{\"soup\":\"쇼유\",\"text\":\" \",\"latitude\":37.556789,\"longitude\":126.923456,\"region\":\"마포구\"}")
            .andExpect(status().isOk()));

        String runId = data.get("recommendationId").asString();
        Map<String, Object> run = jdbc.queryForMap("SELECT * FROM tb_v2_recommendation_run WHERE id = ?",
                Long.valueOf(runId));
        assertThat(run).containsEntry("user_id", member)
            .containsEntry("candidate_count", 3)
            .containsEntry("algorithm_version", "rag-rerank-v1+v2-location-filter");
        assertThat(run.get("latitude").toString()).isEqualTo("37.56");
        assertThat(run.get("longitude").toString()).isEqualTo("126.92");
        assertThat(mapper.readTree(run.get("conditions").toString()).toString())
            .isEqualTo("{\"soup\":\"쇼유\",\"region\":\"마포구\"}");
        assertThat(jdbc.queryForList(
                "SELECT CONCAT(item_rank, ':', shop_id) FROM tb_v2_recommendation_item WHERE run_id = ? ORDER BY item_rank",
                String.class, Long.valueOf(runId)))
            .containsExactly("1:" + NEAR);
    }

    @Test
    void 노출_기록을_남기지_못해도_추천은_성공하고_기록_id만_비운다() throws Exception {
        searchReturns(NEAR);
        doThrow(new IllegalStateException("log down")).when(runs).save(any());

        recommend(active(), "{\"text\":\"돈코츠\"}").andExpect(status().isOk())
            .andExpect(jsonPath("$.data.recommendationId").value(nullValue()))
            .andExpect(jsonPath("$.data.shops[0].id").value(String.valueOf(NEAR)));
    }

    @Test
    void 탈퇴_정리_때_그_회원의_노출_기록만_지운다() throws Exception {
        MobileUser leaving = MobileUser.onboarding("recommend-" + UUID.randomUUID() + "@example.com");
        leaving.completeOnboarding(Nickname.of("R" + UUID.randomUUID().toString().substring(0, 8)), Instant.now());
        Long leavingId = users.saveAndFlush(leaving).getId();
        created.add(leavingId);
        Long staying = active();
        searchReturns(NEAR);
        recommend(leavingId, "{\"text\":\"돈코츠\"}").andExpect(status().isOk());
        recommend(staying, "{\"text\":\"돈코츠\"}").andExpect(status().isOk());
        Instant requestedAt = Instant.parse("2026-09-01T00:00:00Z");
        MobileUser pending = users.findById(leavingId).orElseThrow();
        pending.requestWithdrawal(requestedAt, Duration.ofDays(30));
        users.saveAndFlush(pending);

        purges.purgeExpired(requestedAt.plus(Duration.ofDays(30)));

        assertThat(runCount(leavingId)).isZero();
        assertThat(itemCount(leavingId)).isZero();
        assertThat(runCount(staying)).isEqualTo(1);
        assertThat(itemCount(staying)).isEqualTo(1);
    }

    private int runCount(Long userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM tb_v2_recommendation_run WHERE user_id = ?", Integer.class,
                userId);
    }

    private int itemCount(Long userId) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM tb_v2_recommendation_item item
                JOIN tb_v2_recommendation_run run ON run.id = item.run_id WHERE run.user_id = ?
                """, Integer.class, userId);
    }

    @Test
    void 모두_걸러지면_200_빈_배열이다() throws Exception {
        searchReturns(HIDDEN, DELETED, CLOSED);
        recommend(active(), "{\"text\":\"아무 라멘\"}").andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.shops.length()").value(0));
    }

    @Test
    void 검색_장애와_시간_초과는_503_RECOMMENDATION_UNAVAILABLE이다() throws Exception {
        Long member = active();
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
            .thenThrow(new IllegalStateException("vector down"));
        recommend(member, "{\"text\":\"돈코츠\"}").andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("RECOMMENDATION_UNAVAILABLE"))
            .andExpect(jsonPath("$.meta.requestId").isNotEmpty());

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenAnswer(invocation -> {
            Thread.sleep(2_000);
            return List.of();
        });
        recommend(member, "{\"text\":\"돈코츠\"}").andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.error.code").value("RECOMMENDATION_UNAVAILABLE"));
    }

    @Test
    void 회원당_1분에_10번까지이고_11번째는_429다() throws Exception {
        Long member = active();
        searchReturns(NEAR);
        for (int attempt = 0; attempt < 10; attempt++) {
            recommend(member, "{\"text\":\"돈코츠\"}").andExpect(status().isOk());
        }
        recommend(member, "{\"text\":\"돈코츠\"}").andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.error.code").value("RATE_LIMITED"));
        recommend(active(), "{\"text\":\"돈코츠\"}").andExpect(status().isOk());
    }

    @Test
    void 비회원은_401이고_조건이_없거나_좌표가_반쪽이면_400이다() throws Exception {
        mvc.perform(post("/api/v2/ai/recommendations").contentType(MediaType.APPLICATION_JSON)
            .content("{\"text\":\"돈코츠\"}")).andExpect(status().isUnauthorized());

        Long member = active();
        for (String body : List.of("{}", "{\"soup\":\"  \",\"text\":\"\"}", "{\"text\":\"돈코츠\",\"latitude\":37.5}",
                "{\"text\":\"돈코츠\",\"latitude\":91,\"longitude\":127}", "{\"text\":\"" + "x".repeat(201) + "\"}")) {
            recommend(member, body).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }
    }

    private ResultActions recommend(Long userId, String body) throws Exception {
        return mvc.perform(post("/api/v2/ai/recommendations").header(HttpHeaders.AUTHORIZATION, auth(userId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
    }

    private void searchReturns(long... shopIds) {
        List<Document> documents = new ArrayList<>();
        double score = 0.9;
        for (long shopId : shopIds) {
            documents.add(Document.builder()
                .text("매장 " + shopId)
                .metadata(Map.of("documentType", "SHOP_PROFILE", "shopId", String.valueOf(shopId)))
                .score(score)
                .build());
            score -= 0.05;
        }
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(documents);
    }

    private JsonNode data(ResultActions result) throws Exception {
        return mapper.readTree(result.andReturn().getResponse().getContentAsByteArray()).get("data");
    }

    private List<String> ids(JsonNode data) {
        List<String> ids = new ArrayList<>();
        data.get("shops").forEach(shop -> ids.add(shop.get("id").asString()));
        return ids;
    }

    private void insertShop(long id, String region, String address, String latitude, String longitude, String status,
            boolean published, boolean deleted) {
        String deletedAt = null;
        if (deleted) {
            deletedAt = "2026-09-02 00:00:00";
        }
        jdbc.update("""
                INSERT INTO tb_v2_shop (id, name, branch_name, address, region, latitude, longitude, ramen_types, tags,
                    business_status, ai_summary_keywords, is_published, created_at, updated_at, deleted_at)
                VALUES (?, '추천 매장', '본점', ?, ?, ?, ?, '[]', '[]', ?, '[]', ?, '2026-09-01 00:00:00',
                    '2026-09-01 00:00:00', ?)
                """, id, address, region, latitude, longitude, status, published, deletedAt);
    }

    private Long active() {
        MobileUser user = MobileUser.onboarding("recommend-" + UUID.randomUUID() + "@example.com");
        user.completeOnboarding(Nickname.of("R" + UUID.randomUUID().toString().substring(0, 8)), Instant.now());
        Long id = users.saveAndFlush(user).getId();
        created.add(id);
        return id;
    }

    private String auth(Long userId) {
        return "Bearer " + tokens.issue(userId);
    }

}
