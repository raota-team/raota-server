package com.raota.agent.presentation.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.agent.application.recommendation.ReviewSummaryService;
import com.raota.agent.application.recommendation.query.ReviewSummaryQuery;
import com.raota.agent.presentation.recommendation.response.ReviewSummaryResponse;
import com.raota.agent.presentation.recommendation.response.ReviewSummaryResponse.AiSummary;
import com.raota.agent.presentation.recommendation.response.ReviewSummaryResponse.SummaryDetail;
import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.model.Nickname;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import com.raota.mobile.account.infrastructure.auth.MobileAccessTokenService;
import com.raota.support.BaseIntegrationTest;
import com.raota.web.account.domain.member.model.MemberProfile;
import com.raota.web.account.domain.member.model.MemberRole;
import com.raota.web.account.domain.member.repository.MemberRepository;
import com.raota.web.account.infrastructure.auth.JwtTokenProvider;
import jakarta.persistence.EntityNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

class MobileShopAiSummaryAdminIntegrationTest extends BaseIntegrationTest {

    private static final long SUMMARIZED = 900004801L;

    private static final long NO_REVIEWS = 900004802L;

    private static final long ALREADY = 900004803L;

    private static final long HIDDEN = 900004804L;

    private static final long V1_ONLY = 900004899L;

    @MockitoBean
    private ReviewSummaryService reviewSummaries;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MemberRepository members;

    @Autowired
    private JwtTokenProvider webTokens;

    @Autowired
    private MobileUserRepository mobileUsers;

    @Autowired
    private MobileAccessTokenService mobileTokens;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private StringRedisTemplate redis;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        redis.delete("lock:v2:shop-ai-summary");
        insertShop(SUMMARIZED, true, null);
        insertShop(NO_REVIEWS, true, null);
        insertShop(ALREADY, true, "2026-09-01 00:00:00");
        insertShop(HIDDEN, false, null);
        when(reviewSummaries.summarizeReviews(forShop(SUMMARIZED)))
                .thenReturn(summary(
                        3,
                        new SummaryDetail("진한 국물", "국물이 진하다는 리뷰가 많습니다."),
                        new SummaryDetail("좌석 부족", "좌석이 적어 기다린다는 리뷰가 있습니다."),
                        new SummaryDetail("추천 메뉴 정보 부족", "추천 메뉴를 판단할 수 있는 리뷰 데이터가 충분하지 않습니다.")));
        when(reviewSummaries.summarizeReviews(forShop(NO_REVIEWS)))
                .thenReturn(summary(
                        0,
                        new SummaryDetail("리뷰 데이터 부족", "없음"),
                        new SummaryDetail("리뷰 데이터 부족", "없음"),
                        new SummaryDetail("추천 메뉴 정보 부족", "없음")));
        when(reviewSummaries.summarizeReviews(forShop(HIDDEN))).thenThrow(new EntityNotFoundException("v1 매장 없음"));
        when(reviewSummaries.summarizeReviews(forShop(V1_ONLY)))
                .thenReturn(summary(2, new SummaryDetail("깔끔함", "깔끔하다는 리뷰가 있습니다."), null, null));
    }

    private static ReviewSummaryQuery forShop(long shopId) {
        return argThat(query -> query != null && query.shopId().equals(shopId));
    }

    @AfterEach
    void cleanUp() {
        redis.delete("lock:v2:shop-ai-summary");
        jdbc.update("DELETE FROM tb_v2_shop WHERE id BETWEEN ? AND ?", SUMMARIZED, V1_ONLY);
    }

    @Test
    void 근거_리뷰가_있으면_요약과_키워드를_v2_매장에_저장하고_대체_문구만_뺀다() throws Exception {
        summarize(SUMMARIZED)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shopId").value(SUMMARIZED))
                .andExpect(jsonPath("$.data.outcome").value("SAVED"));

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT ai_review_summary, ai_summary_keywords, ai_summary_generated_at FROM tb_v2_shop WHERE id = ?",
                SUMMARIZED);
        assertThat(row.get("ai_review_summary")).isEqualTo("장점: 국물이 진하다는 리뷰가 많습니다.\n아쉬운 점: 좌석이 적어 기다린다는 리뷰가 있습니다.");
        assertThat(row.get("ai_summary_keywords").toString()).isEqualTo("[\"진한 국물\", \"좌석 부족\"]");
        assertThat(row.get("ai_summary_generated_at")).isNotNull();
        mvc.perform(get("/api/v2/shops/{id}", SUMMARIZED))
                .andExpect(jsonPath("$.data.aiReviewSummary").value(row.get("ai_review_summary")))
                .andExpect(jsonPath("$.data.aiSummaryKeywords[0]").value("진한 국물"));
    }

    @Test
    void 리뷰가_없거나_원본_매장이나_v2_매장이_없으면_저장하지_않고_이유를_알려준다() throws Exception {
        summarize(NO_REVIEWS).andExpect(jsonPath("$.data.outcome").value("NO_REVIEWS"));
        summarize(HIDDEN).andExpect(jsonPath("$.data.outcome").value("NO_SOURCE_SHOP"));
        summarize(V1_ONLY).andExpect(jsonPath("$.data.outcome").value("NO_V2_SHOP"));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM tb_v2_shop WHERE id IN (?, ?) AND ai_summary_generated_at IS NOT NULL",
                        Integer.class,
                        NO_REVIEWS,
                        HIDDEN))
                .isZero();
    }

    @Test
    void 전체_생성은_커서_다음의_요약이_없는_공개_매장만_백그라운드로_처리하고_실행_중에는_거절한다() throws Exception {
        redis.opsForValue().set("lock:v2:shop-ai-summary", "other-server", Duration.ofMinutes(1));
        summarizeMissing(0).andExpect(status().isBadRequest());
        redis.delete("lock:v2:shop-ai-summary");

        summarizeMissing(SUMMARIZED)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.nextAfterId").value(greaterThanOrEqualTo(Math.toIntExact(NO_REVIEWS))));
        awaitBulkFinished();
        assertThat(summarizedIds()).isEmpty();

        summarizeMissing(0)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.queued").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.remaining").value(0));
        awaitBulkFinished();
        assertThat(summarizedIds()).containsExactly(SUMMARIZED);
        assertThat(jdbc.queryForObject(
                        "SELECT ai_summary_generated_at FROM tb_v2_shop WHERE id = ?", String.class, ALREADY))
                .startsWith("2026-09-01");
    }

    private void awaitBulkFinished() {
        await().atMost(Duration.ofSeconds(10))
                .until(() -> !Boolean.TRUE.equals(redis.hasKey("lock:v2:shop-ai-summary")));
    }

    private List<Long> summarizedIds() {
        return jdbc.queryForList(
                "SELECT id FROM tb_v2_shop WHERE id BETWEEN ? AND ? AND ai_review_summary IS NOT NULL",
                Long.class,
                SUMMARIZED,
                V1_ONLY);
    }

    @Test
    void 웹_관리자만_호출할_수_있고_일반_회원과_앱_토큰은_거절한다() throws Exception {
        mvc.perform(post("/admin/api/v2/shops/{shopId}/ai-summary", SUMMARIZED)).andExpect(status().isUnauthorized());
        mvc.perform(post("/admin/api/v2/shops/{shopId}/ai-summary", SUMMARIZED)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + webToken(MemberRole.USER)))
                .andExpect(status().isForbidden());
        MobileUser user = MobileUser.onboarding("summary-" + UUID.randomUUID() + "@example.com");
        user.completeOnboarding(Nickname.of("S" + UUID.randomUUID().toString().substring(0, 8)), Instant.now());
        Long mobileUser = mobileUsers.saveAndFlush(user).getId();
        jdbc.update("UPDATE tb_v2_user SET role = 'ADMIN' WHERE id = ?", mobileUser);
        try {
            mvc.perform(post("/admin/api/v2/shops/{shopId}/ai-summary", SUMMARIZED)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + mobileTokens.issue(mobileUser)))
                    .andExpect(status().isUnauthorized());
        } finally {
            mobileUsers.deleteById(mobileUser);
        }
    }

    private ResultActions summarize(long shopId) throws Exception {
        return mvc.perform(post("/admin/api/v2/shops/{shopId}/ai-summary", shopId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + webToken(MemberRole.ADMIN)));
    }

    private ResultActions summarizeMissing(long afterId) throws Exception {
        return mvc.perform(post("/admin/api/v2/shops/ai-summary")
                .param("afterId", String.valueOf(afterId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + webToken(MemberRole.ADMIN)));
    }

    private String webToken(MemberRole role) {
        MemberProfile member = members.saveAndFlush(
                MemberProfile.builder().nickname("요약 관리자").role(role).build());
        return webTokens.createAccessToken(member.getId());
    }

    private static ReviewSummaryResponse summary(
            int reviewCount, SummaryDetail pros, SummaryDetail cons, SummaryDetail menu) {
        return new ReviewSummaryResponse(null, reviewCount, new AiSummary(pros, cons, menu), List.of());
    }

    private void insertShop(long id, boolean published, String generatedAt) {
        jdbc.update("""
                INSERT INTO tb_v2_shop (id, name, branch_name, address, region, ramen_types, tags, business_status,
                    ai_summary_keywords, ai_summary_generated_at, is_published, created_at, updated_at)
                VALUES (?, '요약 매장', '본점', '서울', '서울', '[]', '[]', 'OPERATIONAL', '[]', ?, ?,
                    '2026-09-01 00:00:00', '2026-09-01 00:00:00')
                """, id, generatedAt, published);
    }
}
