package com.raota.agent.presentation.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.agent.domain.recommendation.repository.MobileRecommendationRunRepository;
import com.raota.global.presentation.common.RequestIdFilter;
import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.model.Nickname;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import com.raota.mobile.account.infrastructure.auth.MobileAccessTokenService;
import com.raota.support.BaseIntegrationTest;
import java.sql.Timestamp;
import java.time.Instant;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** 추천 결과에서 연 매장을 남기고, 관리자 성과 집계가 열기·7일 안 찜·기록을 추천 칸에 이어 붙이는지 본다. */
class MobileRecommendationFeedbackIntegrationTest extends BaseIntegrationTest {

    private static final RequestPostProcessor ADMIN = user("admin").roles("ADMIN");

    private static final long SHOP_A = 900004701L;

    private static final long SHOP_B = 900004702L;

    private static final long SHOP_C = 900004703L;

    private static final long LOG_ID = 900004711L;

    /** 2026-10-10 12:00 KST다. */
    private static final Instant SHOWN_AT = Instant.parse("2026-10-10T03:00:00Z");

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

    private MockMvc mvc;

    private final List<Long> created = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(requestIdFilter).apply(springSecurity()).build();
    }

    @AfterEach
    void cleanUp() {
        for (Long userId : created) {
            runs.deleteByUserId(userId);
            jdbc.update("DELETE FROM tb_v2_shop_bookmark WHERE user_id = ?", userId);
            jdbc.update("DELETE FROM tb_v2_ramen_log WHERE user_id = ?", userId);
            users.deleteById(userId);
        }
    }

    @Test
    void 내_추천의_매장을_열면_처음_연_시각만_남기고_남의_추천이나_없는_매장은_404다() throws Exception {
        Long member = active();
        Long other = active();
        long runId = insertRun(member, SHOWN_AT, "{\"soup\":\"쇼유\"}", SHOP_A, SHOP_B);

        open(member, runId, SHOP_B).andExpect(status().isNoContent());
        Timestamp first = openedAt(runId, SHOP_B);
        assertThat(first).isNotNull();
        open(member, runId, SHOP_B).andExpect(status().isNoContent());
        assertThat(openedAt(runId, SHOP_B)).isEqualTo(first);
        assertThat(openedAt(runId, SHOP_A)).isNull();

        open(other, runId, SHOP_B).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
        open(member, runId, SHOP_C).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
        mvc.perform(post("/api/v2/ai/recommendations/{id}/shops/{shopId}/open", runId, SHOP_B))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void 관리자_성과는_기간_안_추천에_열기와_7일_안_찜_기록을_이어_센다() throws Exception {
        Long member = active();
        long shoyu = insertRun(member, SHOWN_AT, "{\"soup\":\"쇼유\",\"mood\":\"혼밥\"}", SHOP_A, SHOP_B);
        insertRun(member, SHOWN_AT.plusSeconds(60), "{\"text\":\"돈코츠\"}", SHOP_C);
        // 기간(10/01~10/15) 밖 추천은 세지 않는다.
        insertRun(member, Instant.parse("2026-09-01T03:00:00Z"), "{\"soup\":\"쇼유\"}", SHOP_A);
        jdbc.update("UPDATE tb_v2_recommendation_item SET opened_at = ? WHERE run_id = ? AND shop_id = ?",
                Timestamp.from(SHOWN_AT.plusSeconds(5)), shoyu, SHOP_A);
        // B는 하루 뒤 찜, A는 8일 뒤 기록(7일 밖), C는 이틀 뒤 기록.
        jdbc.update("INSERT INTO tb_v2_shop_bookmark (user_id, shop_id, created_at) VALUES (?, ?, ?)", member, SHOP_B,
                Timestamp.from(SHOWN_AT.plusSeconds(86_400)));
        insertLog(LOG_ID, member, SHOP_A, SHOWN_AT.plusSeconds(8 * 86_400L));
        insertLog(LOG_ID + 1, member, SHOP_C, SHOWN_AT.plusSeconds(2 * 86_400L));

        mvc.perform(get("/admin/api/v2/recommendations/stats").param("from", "2026-10-01")
            .param("to", "2026-10-15")
            .with(ADMIN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.attributionDays").value(7))
            .andExpect(jsonPath("$.data.overall.runs").value(2))
            .andExpect(jsonPath("$.data.overall.impressions").value(3))
            .andExpect(jsonPath("$.data.overall.opened").value(1))
            .andExpect(jsonPath("$.data.overall.topOpened").value(1))
            .andExpect(jsonPath("$.data.overall.bookmarked").value(1))
            .andExpect(jsonPath("$.data.overall.logged").value(1))
            .andExpect(jsonPath("$.data.bySoup[0].key").value("(국물 없음)"))
            .andExpect(jsonPath("$.data.bySoup[1].key").value("쇼유"))
            .andExpect(jsonPath("$.data.bySoup[1].bookmarked").value(1))
            .andExpect(jsonPath("$.data.bySoup[1].logged").value(0))
            .andExpect(jsonPath("$.data.byRank[0].key").value("1"))
            .andExpect(jsonPath("$.data.byRank[0].impressions").value(2))
            .andExpect(jsonPath("$.data.byRank[1].key").value("2"))
            .andExpect(jsonPath("$.data.byRank[1].bookmarked").value(1))
            .andExpect(jsonPath("$.data.byAlgorithmVersion[0].key").value("test-version"));
    }

    @Test
    void 관리자_성과_기간이_거꾸로이거나_92일을_넘으면_400이고_관리자만_본다() throws Exception {
        mvc.perform(get("/admin/api/v2/recommendations/stats").param("from", "2026-10-15")
            .param("to", "2026-10-01")
            .with(ADMIN)).andExpect(status().isBadRequest());
        mvc.perform(get("/admin/api/v2/recommendations/stats").param("from", "2026-01-01")
            .param("to", "2026-10-01")
            .with(ADMIN)).andExpect(status().isBadRequest());
        mvc.perform(get("/admin/api/v2/recommendations/stats").with(user("member").roles("USER")))
            .andExpect(status().isForbidden());
    }

    private ResultActions open(Long userId, long runId, long shopId) throws Exception {
        return mvc.perform(post("/api/v2/ai/recommendations/{id}/shops/{shopId}/open", runId, shopId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.issue(userId)));
    }

    private Timestamp openedAt(long runId, long shopId) {
        return jdbc.queryForObject("SELECT opened_at FROM tb_v2_recommendation_item WHERE run_id = ? AND shop_id = ?",
                Timestamp.class, runId, shopId);
    }

    private long insertRun(Long userId, Instant createdAt, String conditions, long... shopIds) {
        jdbc.update("""
                INSERT INTO tb_v2_recommendation_run (user_id, conditions, candidate_count, algorithm_version,
                    index_version, model_version, latency_ms, created_at)
                VALUES (?, ?, 10, 'test-version', 'index', 'model', 100, ?)
                """, userId, conditions, Timestamp.from(createdAt));
        Long runId = jdbc.queryForObject("SELECT MAX(id) FROM tb_v2_recommendation_run WHERE user_id = ?", Long.class,
                userId);
        for (int index = 0; index < shopIds.length; index++) {
            jdbc.update(
                    "INSERT INTO tb_v2_recommendation_item (run_id, item_rank, shop_id, score) VALUES (?, ?, ?, 0.5)",
                    runId, index + 1, shopIds[index]);
        }
        return runId;
    }

    private void insertLog(long id, Long userId, long shopId, Instant createdAt) {
        jdbc.update("""
                INSERT INTO tb_v2_ramen_log (id, user_id, shop_id, visited_at, menu_name, ramen_type,
                    revisit_intention, note, taste_note_codes, visibility, created_at, updated_at)
                VALUES (?, ?, ?, '2026-10-10', '라멘', '쇼유', 'OFTEN', '', '[]', 'PUBLIC', ?, ?)
                """, id, userId, shopId, Timestamp.from(createdAt), Timestamp.from(createdAt));
    }

    private Long active() {
        MobileUser user = MobileUser.onboarding("feedback-" + UUID.randomUUID() + "@example.com");
        user.completeOnboarding(Nickname.of("F" + UUID.randomUUID().toString().substring(0, 8)), Instant.now());
        Long id = users.saveAndFlush(user).getId();
        created.add(id);
        return id;
    }

}
