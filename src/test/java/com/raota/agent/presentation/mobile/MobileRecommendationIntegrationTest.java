package com.raota.agent.presentation.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.agent.application.mobile.port.MobileRecommendationReasonPort;
import com.raota.agent.domain.recommendation.repository.MobileRecommendationRunRepository;
import com.raota.global.presentation.common.RequestIdFilter;
import com.raota.mobile.account.application.service.MobileWithdrawalPurgeService;
import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.model.Nickname;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import com.raota.mobile.account.infrastructure.auth.MobileAccessTokenService;
import com.raota.support.BaseIntegrationTest;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

// 1분 상한(10번)을 확인하려고 같은 회원으로 여러 번 추천받으므로 하루 식권 상한은 따로(MobileRecommendationTicketsIntegrationTest) 본다
@TestPropertySource(
        properties = { "app.mobile.recommendation.timeout=500ms", "app.mobile.recommendation.daily-limit=1000" })
class MobileRecommendationIntegrationTest extends BaseIntegrationTest {

    private static final long NEAR = 900004601L;

    private static final long HIDDEN = 900004602L;

    private static final long DELETED = 900004603L;

    private static final long CLOSED = 900004604L;

    private static final long FAR = 900004605L;

    private static final long NO_COORDINATES = 900004606L;

    private static final long NIBOSHI = 900004607L;

    /** 좌표 순위 확인용 쇼유 매장 10곳의 첫 ID다. NEAR에서 북쪽으로 약 111m씩 떨어진다. */
    private static final long NEARBY_FIRST = 900004611L;

    private static final long LAST = NEARBY_FIRST + 9;

    /** 기기 GPS처럼 소수점이 긴 좌표다. 매장 좌표 정밀도(8자리)로 반올림하면 NEAR와 같은 지점이다. */
    private static final String NEAR_POINT = "\"latitude\":37.55630000412345,\"longitude\":126.92359999876543";

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

    /** 실제 LLM을 부르지 않는다. 따로 정하지 않으면 빈 결과(이유 없음)다. */
    @MockitoBean
    private MobileRecommendationReasonPort reasonWriter;

    private MockMvc mvc;

    private final List<Long> created = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(requestIdFilter).apply(springSecurity()).build();
        insertShop(NEAR, "마포구", "서울 마포구 와우산로 1", "37.55630000", "126.92360000", "OPERATIONAL", true, false, "[\"쇼유\"]");
        insertShop(HIDDEN, "마포구", "서울 마포구 와우산로 2", "37.55630000", "126.92360000", "UNKNOWN", false, false, "[\"쇼유\"]");
        insertShop(DELETED, "마포구", "서울 마포구 와우산로 3", "37.55630000", "126.92360000", "OPERATIONAL", true, true,
                "[\"쇼유\"]");
        insertShop(CLOSED, "마포구", "서울 마포구 와우산로 4", "37.55630000", "126.92360000", "CLOSED", true, false, "[\"쇼유\"]");
        insertShop(FAR, "강남구", "서울 강남구 테헤란로 1", "37.49790000", "127.02760000", "OPERATIONAL", true, false,
                "[\"쇼유\", \"시오\"]");
        insertShop(NO_COORDINATES, "망원동", "서울 마포구 망원로 1", null, null, "TEMPORARILY_CLOSED", true, false, "[\"쇼유\"]");
        insertShop(NIBOSHI, "마포구", "서울 마포구 와우산로 5", "37.55640000", "126.92360000", "OPERATIONAL", true, false,
                "[\"니보시\"]");
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM tb_v2_shop_bookmark WHERE shop_id BETWEEN ? AND ?", NEAR, LAST);
        jdbc.update("DELETE FROM tb_v2_shop_service_perk WHERE shop_id BETWEEN ? AND ?", NEAR, LAST);
        jdbc.update("DELETE FROM tb_v2_shop WHERE id BETWEEN ? AND ?", NEAR, LAST);
        for (Long userId : created) {
            runs.deleteByUserId(userId);
        }
        created.forEach(users::deleteById);
    }

    @Test
    void 고른_국물의_공개되고_영업하는_매장만_유사도_순으로_돌려주고_v2_북마크와_상세로_이어진다() throws Exception {
        Long member = active();
        jdbc.update(
                "INSERT INTO tb_v2_shop_bookmark (user_id, shop_id, created_at) VALUES (?, ?, CURRENT_TIMESTAMP(6))",
                member, FAR);
        searchReturns(NIBOSHI, FAR, NEAR, HIDDEN, DELETED, CLOSED, NO_COORDINATES);

        JsonNode data = data(
                recommend(member, "{\"soup\":\"쇼유\",\"mood\":\"혼밥\",\"text\":\"차슈 많은 곳\"}").andExpect(status().isOk()));

        assertThat(ids(data)).containsExactly(String.valueOf(FAR), String.valueOf(NEAR));
        assertThat(data.get("shops").get(0).get("isBookmarked").asBoolean()).isTrue();
        assertThat(data.get("shops").get(1).get("isBookmarked").asBoolean()).isFalse();
        assertThat(data.get("shops").get(1).get("distanceMeters").isNull()).isTrue();
        assertThat(data.get("appliedConditions").toString()).isEqualTo("[\"쇼유\",\"혼밥\"]");
        mvc.perform(get("/api/v2/shops/{shopId}", data.get("shops").get(0).get("id").asString()))
            .andExpect(status().isOk());
    }

    @Test
    void 보여주는_매장마다_LLM이_쓴_추천_이유를_한_줄로_돌려주고_지어낸_매장_이유는_버린다() throws Exception {
        searchReturns(FAR, NEAR);
        jdbc.update("UPDATE tb_v2_shop SET ai_review_summary = '맑은 간장 국물과 얇은 면', price_min = 9000, price_max = 11000 "
                + "WHERE id = ?", FAR);
        jdbc.update("""
                INSERT INTO tb_v2_shop_service_perk (shop_id, perk_type, status, price, condition_text) VALUES
                (?, 'NOODLE_REFILL', 'FREE', NULL, '1회'), (?, 'RICE_REFILL', 'PAID', 1000, NULL),
                (?, 'SOUP_REFILL', 'UNKNOWN', NULL, NULL)
                """, FAR, FAR, FAR);
        when(reasonWriter.reasons(any(), any()))
            .thenReturn(Map.of(String.valueOf(FAR), "  진한 쇼유를\n 하는 곳이에요 ", "999", "목록에 없는 매장"));

        JsonNode data = data(recommend(active(), "{\"soup\":\"쇼유\",\"mood\":\"혼밥\"}").andExpect(status().isOk()));

        assertThat(ids(data)).containsExactly(String.valueOf(FAR), String.valueOf(NEAR));
        assertThat(data.get("reasons").toString()).isEqualTo("{\"" + FAR + "\":\"진한 쇼유를 하는 곳이에요\"}");
        ArgumentCaptor<String> conditions = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> context = ArgumentCaptor.forClass(String.class);
        verify(reasonWriter).reasons(conditions.capture(), context.capture());
        assertThat(conditions.getValue()).contains("국물: 쇼유", "상황: 혼밥");
        assertThat(context.getValue())
            .contains("매장 ID: " + FAR, "매장 ID: " + NEAR, "리뷰 요약: 맑은 간장 국물과 얇은 면", "가격대: 9000~11000원",
                    "서비스: 면 추가 무료(1회), 공깃밥 1000원")
            .doesNotContain("국물 추가");
    }

    @Test
    void 추천_이유를_만들지_못해도_매장은_그대로_추천한다() throws Exception {
        searchReturns(NEAR);
        when(reasonWriter.reasons(any(), any())).thenThrow(new IllegalStateException("llm down"));

        recommend(active(), "{\"soup\":\"쇼유\",\"mood\":\"혼밥\"}").andExpect(status().isOk())
            .andExpect(jsonPath("$.data.shops[0].id").value(String.valueOf(NEAR)))
            .andExpect(jsonPath("$.data.reasons").isEmpty());
    }

    @Test
    void 검색에_걸리지_않아도_고른_국물의_매장이면_추천한다() throws Exception {
        searchReturns(FAR, NEAR);

        JsonNode data = data(recommend(active(), "{\"soup\":\"니보시\",\"mood\":\"혼밥\"}").andExpect(status().isOk()));

        assertThat(ids(data)).containsExactly(String.valueOf(NIBOSHI));
    }

    @Test
    void 지역은_그_안의_매장만_남기고_좌표는_반경_없이_가까운_순으로_조건에_표시한다() throws Exception {
        Long member = active();
        searchReturns();

        JsonNode byRegion = data(recommend(member, "{\"soup\":\"쇼유\",\"region\":\"마포구\"}").andExpect(status().isOk()));
        assertThat(ids(byRegion)).containsExactly(String.valueOf(NEAR));
        assertThat(byRegion.get("appliedConditions").toString()).isEqualTo("[\"쇼유\",\"마포구\"]");

        JsonNode byPoint = data(recommend(member, "{\"soup\":\"쇼유\"," + NEAR_POINT + "}").andExpect(status().isOk()));
        assertThat(ids(byPoint)).containsExactly(String.valueOf(NEAR), String.valueOf(FAR));
        assertThat(byPoint.get("shops").get(0).get("distanceMeters").asInt()).isZero();
        assertThat(byPoint.get("shops").get(1).get("distanceMeters").asInt()).isGreaterThan(3_000);
        assertThat(byPoint.get("appliedConditions").toString()).isEqualTo("[\"쇼유\",\"가까운 순\"]");
    }

    @Test
    void 좌표가_오면_가까운_10곳만_후보이고_유사도에_가까운_순위만큼_점수를_더한다() throws Exception {
        for (int index = 0; index < 10; index++) {
            String latitude = new BigDecimal("37.55630000")
                .add(new BigDecimal("0.001").multiply(BigDecimal.valueOf(index + 1)))
                .toPlainString();
            insertShop(NEARBY_FIRST + index, "마포구", "서울 마포구 근처로 " + index, latitude, "126.92360000", "OPERATIONAL",
                    true, false, "[\"쇼유\"]");
        }
        // 후보는 NEAR와 근처 9곳(가까운 순)이다. 가장 먼 근처 매장과 FAR는 유사도가 높아도 후보 밖이다.
        searchReturns(FAR, NEARBY_FIRST + 9, NEARBY_FIRST + 8);

        JsonNode data = data(
                recommend(active(), "{\"soup\":\"쇼유\",\"mood\":\"혼밥\"," + NEAR_POINT + "}").andExpect(status().isOk()));

        assertThat(ids(data)).containsExactly(String.valueOf(NEARBY_FIRST + 8), String.valueOf(NEAR),
                String.valueOf(NEARBY_FIRST), String.valueOf(NEARBY_FIRST + 1), String.valueOf(NEARBY_FIRST + 2),
                String.valueOf(NEARBY_FIRST + 3));
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
            .containsEntry("candidate_count", 1)
            .containsEntry("algorithm_version", "soup-filter+nearest10+vector-similarity");
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
        recommend(leavingId, "{\"soup\":\"니보시\"}").andExpect(status().isOk());
        recommend(staying, "{\"soup\":\"니보시\"}").andExpect(status().isOk());
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
    void 고른_국물의_추천할_매장이_없으면_200_빈_배열이다() throws Exception {
        searchReturns(NEAR);
        recommend(active(), "{\"soup\":\"지로\",\"mood\":\"혼밥\"}").andExpect(status().isOk())
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
    void 동시_검색이_상한에_닿으면_검색을_시작하지_않고_503이다() throws Exception {
        Long member = active();
        AtomicBoolean released = new AtomicBoolean(false);
        AtomicInteger started = new AtomicInteger();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenAnswer(invocation -> {
            started.incrementAndGet();
            // 시간 초과 인터럽트를 무시하고 외부 호출이 늦게 끝나는 상황을 만든다.
            while (!released.get()) {
                LockSupport.parkNanos(10_000_000L);
            }
            return List.of();
        });
        try {
            for (int attempt = 0; attempt < 8; attempt++) {
                recommend(member, "{\"text\":\"돈코츠\"}").andExpect(status().isServiceUnavailable());
            }
            recommend(member, "{\"text\":\"돈코츠\"}").andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("RECOMMENDATION_UNAVAILABLE"));
            // 시간 초과된 검색도 자리를 잡은 채 늦게 시작할 수 있어 8개가 모두 시작할 때까지 기다린 뒤, 9번째는 시작하지 않았음을 본다.
            await().atMost(Duration.ofSeconds(5)).until(() -> started.get() == 8);
            Thread.sleep(300);
            assertThat(started.get()).isEqualTo(8);
        }
        finally {
            released.set(true);
        }
        searchReturns(NEAR);
        Long other = active();
        await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> recommend(other, "{\"text\":\"돈코츠\"}").andExpect(status().isOk()));
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
            boolean published, boolean deleted, String ramenTypes) {
        String deletedAt = null;
        if (deleted) {
            deletedAt = "2026-09-02 00:00:00";
        }
        jdbc.update("""
                INSERT INTO tb_v2_shop (id, name, branch_name, address, region, latitude, longitude, ramen_types, tags,
                    business_status, ai_summary_keywords, is_published, created_at, updated_at, deleted_at)
                VALUES (?, '추천 매장', '본점', ?, ?, ?, ?, ?, '[]', ?, '[]', ?, '2026-09-01 00:00:00',
                    '2026-09-01 00:00:00', ?)
                """, id, address, region, latitude, longitude, ramenTypes, status, published, deletedAt);
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
