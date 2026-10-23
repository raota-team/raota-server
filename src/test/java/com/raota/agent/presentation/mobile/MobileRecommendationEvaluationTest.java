package com.raota.agent.presentation.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.agent.application.mobile.port.MobileRecommendationReasonPort;
import com.raota.agent.domain.recommendation.repository.MobileRecommendationRunRepository;
import com.raota.global.presentation.common.RequestIdFilter;
import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.model.Nickname;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import com.raota.mobile.account.infrastructure.auth.MobileAccessTokenService;
import com.raota.support.BaseIntegrationTest;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * v2 AI 추천 평가셋({@code evaluation/mobile-recommendation-v2.json})을 실제 추천 API로 돌린다. 벡터 유사도는
 * 매장마다 고정하고 LLM 이유는 부르지 않는다.
 *
 * <p>
 * 모든 케이스에서 같은 규칙을 본다: 보여준 매장은 모두 고른 국물이고 추천해도 되는 매장이며, 좌표가 있으면 그 국물의 가장 가까운 10곳 안에서만 고르고,
 * 고를 수 있는 만큼(최대 6곳) 채운다. 케이스마다 기대 1위와 빠져야 할 매장을 더 본다.
 * </p>
 */
class MobileRecommendationEvaluationTest extends BaseIntegrationTest {

    private static final String DATASET = "evaluation/mobile-recommendation-v2.json";

    private static final long FIRST_SHOP_ID = 900005001L;

    private static final int MAX_SHOPS = 6;

    private static final int NEAREST_CANDIDATES = 10;

    /** 위도 1도의 거리(m)다. 북쪽으로 떨어진 거리만 쓰므로 경도는 그대로 둔다. */
    private static final double METERS_PER_LATITUDE_DEGREE = 111_320;

    private static final ObjectMapper JSON = JsonMapper.builder().build();

    private static final Dataset DATA = load();

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

    /** 실제 LLM을 부르지 않는다. 빈 결과(이유 없음)다. */
    @MockitoBean
    private MobileRecommendationReasonPort reasonWriter;

    private MockMvc mvc;

    private final List<Long> created = new ArrayList<>();

    static Stream<EvaluationCase> cases() {
        return DATA.cases().stream();
    }

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(requestIdFilter).apply(springSecurity()).build();
        List<Document> documents = new ArrayList<>();
        for (int index = 0; index < DATA.shops().size(); index++) {
            FixtureShop shop = DATA.shops().get(index);
            long id = FIRST_SHOP_ID + index;
            insertShop(id, shop);
            documents.add(Document.builder()
                .text(shop.key())
                .metadata(Map.of("documentType", "SHOP_PROFILE", "shopId", String.valueOf(id)))
                .score(shop.similarity())
                .build());
        }
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(documents);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM tb_v2_shop WHERE id BETWEEN ? AND ?", FIRST_SHOP_ID,
                FIRST_SHOP_ID + DATA.shops().size() - 1);
        for (Long userId : created) {
            runs.deleteByUserId(userId);
            users.deleteById(userId);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void 평가셋_케이스(EvaluationCase evaluation) throws Exception {
        JsonNode data = recommend(evaluation);
        List<String> shown = new ArrayList<>();
        data.get("shops").forEach(shop -> shown.add(keyOf(Long.parseLong(shop.get("id").asString()))));

        if (Boolean.TRUE.equals(evaluation.expect().empty())) {
            assertThat(shown).as("%s: 고를 매장이 없어 비어야 한다", evaluation.caseId()).isEmpty();
            return;
        }
        List<FixtureShop> eligible = eligible(evaluation);
        Set<String> eligibleKeys = eligible.stream().map(FixtureShop::key).collect(Collectors.toSet());
        assertThat(shown).as("%s: 고를 수 있는 만큼(최대 %d곳) 채운다", evaluation.caseId(), MAX_SHOPS)
            .hasSize(Math.min(MAX_SHOPS, eligible.size()));
        assertThat(eligibleKeys)
            .as("%s: 고른 국물의 추천해도 되는 매장(좌표가 있으면 가까운 %d곳)만 보여준다", evaluation.caseId(), NEAREST_CANDIDATES)
            .containsAll(shown);
        if (evaluation.expect().top1() != null) {
            assertThat(shown.getFirst()).as("%s: 1위", evaluation.caseId()).isEqualTo(evaluation.expect().top1());
        }
        if (evaluation.expect().excluded() != null) {
            assertThat(shown).as("%s: 빠져야 할 매장", evaluation.caseId())
                .doesNotContainAnyElementsOf(evaluation.expect().excluded());
        }
    }

    /** 평가셋 정의대로 이 케이스에서 고를 수 있는 매장이다. 구현을 따라 하지 않고 규칙만 적는다. */
    private static List<FixtureShop> eligible(EvaluationCase evaluation) {
        String soup = evaluation.request().get("soup");
        Stream<FixtureShop> matching = DATA.shops()
            .stream()
            .filter(shop -> shop.recommendable() && shop.ramenTypes().contains(soup));
        if (!evaluation.located()) {
            return matching.toList();
        }
        return matching.sorted(Comparator.comparingInt(FixtureShop::northMeters)).limit(NEAREST_CANDIDATES).toList();
    }

    private JsonNode recommend(EvaluationCase evaluation) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>(evaluation.request());
        body.put("region", DATA.region());
        if (evaluation.located()) {
            body.put("latitude", DATA.origin().latitude());
            body.put("longitude", DATA.origin().longitude());
        }
        byte[] response = mvc
            .perform(post("/api/v2/ai/recommendations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.issue(active()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
        return JSON.readTree(response).get("data");
    }

    private void insertShop(long id, FixtureShop shop) {
        BigDecimal latitude = DATA.origin()
            .latitude()
            .add(BigDecimal.valueOf(shop.northMeters() / METERS_PER_LATITUDE_DEGREE))
            .setScale(8, RoundingMode.HALF_UP);
        String deletedAt = null;
        if (Boolean.TRUE.equals(shop.deleted())) {
            deletedAt = "2026-09-02 00:00:00";
        }
        jdbc.update("""
                INSERT INTO tb_v2_shop (id, name, branch_name, address, region, latitude, longitude, ramen_types, tags,
                    business_status, ai_summary_keywords, is_published, created_at, updated_at, deleted_at)
                VALUES (?, ?, NULL, ?, ?, ?, ?, ?, '[]', ?, '[]', ?, '2026-09-01 00:00:00', '2026-09-01 00:00:00', ?)
                """, id, shop.key(), "서울 " + DATA.region() + " 평가로 " + id, DATA.region(), latitude,
                DATA.origin().longitude(), JSON.writeValueAsString(shop.ramenTypes()), shop.statusOrDefault(),
                !Boolean.FALSE.equals(shop.published()), deletedAt);
    }

    private String keyOf(long shopId) {
        return DATA.shops().get(Math.toIntExact(shopId - FIRST_SHOP_ID)).key();
    }

    private Long active() {
        MobileUser user = MobileUser.onboarding("evaluation-" + UUID.randomUUID() + "@example.com");
        user.completeOnboarding(Nickname.of("E" + UUID.randomUUID().toString().substring(0, 8)), Instant.now());
        Long id = users.saveAndFlush(user).getId();
        created.add(id);
        return id;
    }

    private static Dataset load() {
        try (InputStream input = new ClassPathResource(DATASET).getInputStream()) {
            Dataset dataset = JSON.readValue(input, Dataset.class);
            Map<String, Integer> keys = new HashMap<>();
            dataset.shops().forEach(shop -> keys.merge(shop.key(), 1, Integer::sum));
            keys.forEach((key, count) -> assertThat(count).as("평가셋 매장 키 중복: %s", key).isEqualTo(1));
            return dataset;
        }
        catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    record Dataset(String version, String description, String region, Origin origin, List<FixtureShop> shops,
            List<EvaluationCase> cases) {
    }

    record Origin(BigDecimal latitude, BigDecimal longitude) {
    }

    /** {@code status}·{@code published}·{@code deleted}를 비우면 영업 중·공개·미삭제다. */
    record FixtureShop(String key, List<String> ramenTypes, int northMeters, double similarity, String status,
            Boolean published, Boolean deleted) {

        String statusOrDefault() {
            if (status == null) {
                return "OPERATIONAL";
            }
            return status;
        }

        boolean recommendable() {
            return !Boolean.FALSE.equals(published) && !Boolean.TRUE.equals(deleted)
                    && !"CLOSED".equals(statusOrDefault()) && !"TEMPORARILY_CLOSED".equals(statusOrDefault());
        }

    }

    record EvaluationCase(String caseId, Map<String, String> request, boolean located, Expect expect, String note) {

        @Override
        public String toString() {
            return caseId;
        }

    }

    record Expect(String top1, List<String> excluded, Boolean empty) {
    }

}
