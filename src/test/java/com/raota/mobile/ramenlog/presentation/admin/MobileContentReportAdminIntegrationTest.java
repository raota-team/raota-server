package com.raota.mobile.ramenlog.presentation.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.model.Nickname;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import com.raota.support.BaseIntegrationTest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 관리자 웹의 라운지 신고 처리: 대상 미리보기와 비공개·삭제 대상 가림, 같은 대상 신고 일괄 종료, 삭제 시 집계, 비공개 기록 삭제·중복 처리 거절을 확인한다.
 */
class MobileContentReportAdminIntegrationTest extends BaseIntegrationTest {

    private static final String BASE = "/admin/api/v2/reports";

    private static final RequestPostProcessor ADMIN = user("admin").roles("ADMIN");

    private static final long SHOP = 900006701L;

    private static final long LOG = 900006711L;

    private static final long COMMENT = 900006721L;

    private static final long LOG_REPORT_FIRST = 900006731L;

    private static final long LOG_REPORT_SECOND = 900006732L;

    private static final long COMMENT_REPORT = 900006733L;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MobileUserRepository users;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;

    private final List<Long> created = new ArrayList<>();

    private Long author;

    private Long commenter;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        jdbc.update("""
                INSERT INTO tb_v2_shop (id, name, branch_name, address, region, ramen_types, tags, business_status,
                    ai_summary_keywords, log_count, satisfaction_score_sum, scored_log_count, is_published, created_at,
                    updated_at)
                VALUES (?, '신고처리 매장', '본점', '서울', '서울', '[]', '[]', 'OPERATIONAL', '[]', 1, 4, 1, TRUE,
                    '2026-09-01 00:00:00', '2026-09-01 00:00:00')
                """, SHOP);
        author = active("작성자");
        commenter = active("댓글러");
        Long firstReporter = active("신고자A");
        Long secondReporter = active("신고자B");
        jdbc.update("UPDATE tb_v2_user SET log_count = 1 WHERE id = ?", author);
        jdbc.update("""
                INSERT INTO tb_v2_ramen_log (id, user_id, shop_id, visited_at, menu_name, ramen_type,
                    satisfaction_score, broth_density_score, noodle_firmness_score, topping_score,
                    revisit_intention, note, taste_note_codes, visibility, comment_count, created_at, updated_at)
                VALUES (?, ?, ?, '2026-09-01', '특제 쇼유', '쇼유', 4, 3, 3, 3, 'OFTEN', '광고 글입니다', '[]', 'PUBLIC', 1,
                    '2026-09-01 00:00:00', '2026-09-01 00:00:00')
                """, LOG, author, SHOP);
        jdbc.update("""
                INSERT INTO tb_v2_ramen_log_image (ramen_log_id, url, sort_order)
                VALUES (?, 'https://images.raota.net/log.jpg', 0)
                """, LOG);
        jdbc.update("""
                INSERT INTO tb_v2_ramen_log_comment (id, ramen_log_id, user_id, content, created_at, updated_at)
                VALUES (?, ?, ?, '욕설 댓글', '2026-09-01 00:00:00', '2026-09-01 00:00:00')
                """, COMMENT, LOG, commenter);
        report(LOG_REPORT_FIRST, firstReporter, "RAMEN_LOG", LOG, "SPAM", "2026-09-02 00:00:00");
        report(LOG_REPORT_SECOND, secondReporter, "RAMEN_LOG", LOG, "OTHER", "2026-09-02 02:00:00");
        report(COMMENT_REPORT, firstReporter, "RAMEN_LOG_COMMENT", COMMENT, "ABUSE", "2026-09-02 01:00:00");
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM tb_v2_content_report WHERE id BETWEEN ? AND ?", LOG_REPORT_FIRST, COMMENT_REPORT);
        jdbc.update("DELETE FROM tb_v2_ramen_log_comment WHERE ramen_log_id = ?", LOG);
        jdbc.update("DELETE FROM tb_v2_ramen_log_image WHERE ramen_log_id = ?", LOG);
        jdbc.update("DELETE FROM tb_v2_ramen_log WHERE id = ?", LOG);
        jdbc.update("DELETE FROM tb_v2_shop WHERE id = ?", SHOP);
        created.forEach(users::deleteById);
    }

    @Test
    void 열린_신고를_오래된_순으로_대상_미리보기와_같은_대상_신고_수와_함께_보여준다() throws Exception {
        mvc.perform(get(BASE).with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(3))
                .andExpect(jsonPath("$.data.items[0].id").value(String.valueOf(LOG_REPORT_FIRST)))
                .andExpect(jsonPath("$.data.items[0].reporter.nickname").value(startsWith("신고자A")))
                .andExpect(jsonPath("$.data.items[0].openReportCount").value(2))
                .andExpect(jsonPath("$.data.items[0].target.type").value("RAMEN_LOG"))
                .andExpect(jsonPath("$.data.items[0].target.author.nickname").value(startsWith("작성자")))
                .andExpect(jsonPath("$.data.items[0].target.text").value("광고 글입니다"))
                .andExpect(jsonPath("$.data.items[0].target.menuName").value("특제 쇼유"))
                .andExpect(jsonPath("$.data.items[0].target.imageUrl").value("https://images.raota.net/log.jpg"))
                .andExpect(jsonPath("$.data.items[0].target.deleted").value(false))
                .andExpect(jsonPath("$.data.items[0].target.hidden").value(false))
                .andExpect(jsonPath("$.data.items[1].id").value(String.valueOf(COMMENT_REPORT)))
                .andExpect(jsonPath("$.data.items[1].openReportCount").value(1))
                .andExpect(jsonPath("$.data.items[1].target.ramenLogId").value(String.valueOf(LOG)))
                .andExpect(jsonPath("$.data.items[1].target.text").value("욕설 댓글"))
                .andExpect(jsonPath("$.data.items[1].target.author.nickname").value(startsWith("댓글러")))
                .andExpect(jsonPath("$.data.items[1].target.hidden").value(false))
                .andExpect(jsonPath("$.data.page.totalElements").value(3));
    }

    @Test
    void 기록을_지우면_같은_대상_신고를_모두_닫고_회원과_매장_집계를_줄인다() throws Exception {
        resolve(LOG_REPORT_SECOND, "DELETE_CONTENT")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.closedReportCount").value(2));

        assertThat(jdbc.queryForObject(
                        "SELECT deleted_at IS NOT NULL FROM tb_v2_ramen_log WHERE id = ?", Boolean.class, LOG))
                .isTrue();
        assertThat(jdbc.queryForObject("SELECT log_count FROM tb_v2_shop WHERE id = ?", Integer.class, SHOP))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT satisfaction_score_sum FROM tb_v2_shop WHERE id = ?", Integer.class, SHOP))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT log_count FROM tb_v2_user WHERE id = ?", Integer.class, author))
                .isZero();
        assertThat(statuses()).containsExactly("RESOLVED", "RESOLVED", "OPEN");

        resolve(LOG_REPORT_FIRST, "DISMISS")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
        mvc.perform(get(BASE).param("status", "RESOLVED").with(ADMIN))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].target.deleted").value(true))
                .andExpect(jsonPath("$.data.items[0].target.hidden").value(true))
                .andExpect(jsonPath("$.data.items[0].target.text").value(nullValue()))
                .andExpect(jsonPath("$.data.items[0].target.menuName").value(nullValue()))
                .andExpect(jsonPath("$.data.items[0].target.imageUrl").value(nullValue()))
                .andExpect(jsonPath("$.data.items[0].target.author.nickname").value(startsWith("작성자")))
                .andExpect(jsonPath("$.data.items[0].resolvedAt").isNotEmpty());
        mvc.perform(get(BASE).with(ADMIN))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(String.valueOf(COMMENT_REPORT)))
                .andExpect(jsonPath("$.data.items[0].target.deleted").value(false))
                .andExpect(jsonPath("$.data.items[0].target.hidden").value(true))
                .andExpect(jsonPath("$.data.items[0].target.text").value(nullValue()));
    }

    @Test
    void 비공개로_바뀐_기록과_그_댓글은_미리보기를_가리고_기록_삭제는_거절하며_기각은_받는다() throws Exception {
        jdbc.update("UPDATE tb_v2_ramen_log SET visibility = 'PRIVATE' WHERE id = ?", LOG);

        mvc.perform(get(BASE).with(ADMIN))
                .andExpect(jsonPath("$.data.items[0].id").value(String.valueOf(LOG_REPORT_FIRST)))
                .andExpect(jsonPath("$.data.items[0].target.hidden").value(true))
                .andExpect(jsonPath("$.data.items[0].target.deleted").value(false))
                .andExpect(jsonPath("$.data.items[0].target.text").value(nullValue()))
                .andExpect(jsonPath("$.data.items[0].target.menuName").value(nullValue()))
                .andExpect(jsonPath("$.data.items[0].target.imageUrl").value(nullValue()))
                .andExpect(jsonPath("$.data.items[0].target.author.nickname").value(startsWith("작성자")))
                .andExpect(jsonPath("$.data.items[0].openReportCount").value(2))
                .andExpect(jsonPath("$.data.items[1].id").value(String.valueOf(COMMENT_REPORT)))
                .andExpect(jsonPath("$.data.items[1].target.hidden").value(true))
                .andExpect(jsonPath("$.data.items[1].target.deleted").value(false))
                .andExpect(jsonPath("$.data.items[1].target.text").value(nullValue()))
                .andExpect(jsonPath("$.data.items[1].target.ramenLogId").value(String.valueOf(LOG)));

        resolve(LOG_REPORT_FIRST, "DELETE_CONTENT")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        assertThat(jdbc.queryForObject(
                        "SELECT deleted_at IS NULL FROM tb_v2_ramen_log WHERE id = ?", Boolean.class, LOG))
                .isTrue();
        assertThat(statuses()).containsOnly("OPEN");

        resolve(LOG_REPORT_FIRST, "DISMISS")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.closedReportCount").value(2));
        assertThat(statuses()).containsExactly("DISMISSED", "DISMISSED", "OPEN");
    }

    @Test
    void 댓글을_지우면_기록의_댓글_수를_줄이고_기록은_남긴다() throws Exception {
        resolve(COMMENT_REPORT, "DELETE_CONTENT")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.closedReportCount").value(1));

        assertThat(jdbc.queryForObject(
                        "SELECT deleted_at IS NOT NULL FROM tb_v2_ramen_log_comment WHERE id = ?",
                        Boolean.class,
                        COMMENT))
                .isTrue();
        assertThat(jdbc.queryForObject("SELECT comment_count FROM tb_v2_ramen_log WHERE id = ?", Integer.class, LOG))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT deleted_at IS NULL FROM tb_v2_ramen_log WHERE id = ?", Boolean.class, LOG))
                .isTrue();
        assertThat(statuses()).containsExactly("OPEN", "OPEN", "RESOLVED");
    }

    @Test
    void 기각하면_대상은_두고_같은_대상_신고를_기각으로_닫는다() throws Exception {
        resolve(LOG_REPORT_FIRST, "DISMISS")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.closedReportCount").value(2));

        assertThat(statuses()).containsExactly("DISMISSED", "DISMISSED", "OPEN");
        assertThat(jdbc.queryForObject(
                        "SELECT deleted_at IS NULL FROM tb_v2_ramen_log WHERE id = ?", Boolean.class, LOG))
                .isTrue();
        assertThat(jdbc.queryForObject("SELECT log_count FROM tb_v2_shop WHERE id = ?", Integer.class, SHOP))
                .isEqualTo(1);
    }

    @Test
    void 없는_신고와_잘못된_처리는_거절하고_웹_관리자만_쓸_수_있다() throws Exception {
        resolve(900006799L, "DISMISS").andExpect(status().isNotFound());
        mvc.perform(post(BASE + "/" + LOG_REPORT_FIRST + "/resolve")
                        .with(ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"BAN\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE).with(user("member").roles("USER"))).andExpect(status().isForbidden());
        assertThat(statuses()).containsOnly("OPEN");
    }

    private ResultActions resolve(long reportId, String action) throws Exception {
        return mvc.perform(post(BASE + "/" + reportId + "/resolve")
                .with(ADMIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"" + action + "\"}"));
    }

    private List<String> statuses() {
        return jdbc.queryForList(
                "SELECT status FROM tb_v2_content_report WHERE id BETWEEN ? AND ? ORDER BY id",
                String.class,
                LOG_REPORT_FIRST,
                COMMENT_REPORT);
    }

    private void report(long id, Long reporterId, String targetType, long targetId, String reason, String createdAt) {
        jdbc.update("""
                INSERT INTO tb_v2_content_report (id, reporter_id, target_type, target_id, reason, status, created_at)
                VALUES (?, ?, ?, ?, ?, 'OPEN', ?)
                """, id, reporterId, targetType, targetId, reason, createdAt);
    }

    private Long active(String nickname) {
        MobileUser user = MobileUser.onboarding("report-admin-" + UUID.randomUUID() + "@example.com");
        user.completeOnboarding(
                Nickname.of(nickname + UUID.randomUUID().toString().substring(0, 4)), Instant.now());
        Long id = users.saveAndFlush(user).getId();
        created.add(id);
        return id;
    }
}
