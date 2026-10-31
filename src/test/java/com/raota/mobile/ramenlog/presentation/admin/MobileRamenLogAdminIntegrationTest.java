package com.raota.mobile.ramenlog.presentation.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 관리자 웹의 앱 기록 관리: 비공개 기록 가림과 공개 기록만 찾는 검색, 삭제 기록 제외, 회원별 개수, 공개 기록만 운영자 삭제하는 것을 확인한다.
 */
class MobileRamenLogAdminIntegrationTest extends BaseIntegrationTest {

    private static final String BASE = "/admin/api/v2/ramen-logs";

    private static final RequestPostProcessor ADMIN = user("admin").roles("ADMIN");

    private static final long SHOP = 900006801L;

    private static final long LOG_PUBLIC = 900006811L;

    private static final long LOG_PRIVATE = 900006812L;

    private static final long LOG_DELETED = 900006813L;

    private static final long LOG_OTHER = 900006814L;

    private static final long REPORT_FIRST = 900006831L;

    private static final long REPORT_SECOND = 900006832L;

    private static final long REPORT_PRIVATE = 900006833L;

    private static final List<String> ROW_HIDDEN_FIELDS = List.of(
            "shop",
            "menuName",
            "ramenType",
            "visitedAt",
            "note",
            "thumbnailUrl",
            "imageCount",
            "likeCount",
            "commentCount",
            "openReportCount",
            "updatedAt");

    private static final List<String> DETAIL_HIDDEN_FIELDS =
            List.of("imageUrls", "tasteNotes", "revisitIntention", "scores");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MobileUserRepository users;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;

    private final List<Long> created = new ArrayList<>();

    private Long author;

    private Long other;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        jdbc.update("""
                INSERT INTO tb_v2_shop (id, name, branch_name, address, region, ramen_types, tags, business_status,
                    ai_summary_keywords, log_count, satisfaction_score_sum, scored_log_count, is_published, created_at,
                    updated_at)
                VALUES (?, '기록관리 매장', '본점', '서울', '서울', '[]', '[]', 'OPERATIONAL', '[]', 3, 8, 2, FALSE,
                    '2026-09-01 00:00:00', '2026-09-01 00:00:00')
                """, SHOP);
        author = active("작성자");
        other = active("다른회원");
        Long firstReporter = active("신고자A");
        Long secondReporter = active("신고자B");
        jdbc.update("UPDATE tb_v2_user SET log_count = 2 WHERE id = ?", author);
        jdbc.update("UPDATE tb_v2_user SET log_count = 1 WHERE id = ?", other);
        scoredLog(LOG_PUBLIC, author, "특제 쇼유", "맑은 국물", "PUBLIC", "2026-09-01 00:00:00", null);
        scoredLog(LOG_PRIVATE, author, "돈코츠", "진한 비밀 메모", "PRIVATE", "2026-09-02 00:00:00", null);
        scoredLog(LOG_DELETED, author, "시오", "지운 기록", "PUBLIC", "2026-09-03 00:00:00", "2026-09-05 00:00:00");
        jdbc.update("""
                INSERT INTO tb_v2_ramen_log (id, user_id, shop_id, visited_at, menu_name, ramen_type, note,
                    taste_note_codes, visibility, created_at, updated_at)
                VALUES (?, ?, ?, '2026-09-04', '미소 라멘', '미소', '옮겨 온 기록', '[]', 'PRIVATE',
                    '2026-09-04 00:00:00', '2026-09-04 00:00:00')
                """, LOG_OTHER, other, SHOP);
        image(LOG_PUBLIC, "https://images.raota.net/second.jpg", 1);
        image(LOG_PUBLIC, "https://images.raota.net/first.jpg", 0);
        image(LOG_PRIVATE, "https://images.raota.net/private.jpg", 0);
        report(REPORT_FIRST, firstReporter, LOG_PUBLIC);
        report(REPORT_SECOND, secondReporter, LOG_PUBLIC);
        report(REPORT_PRIVATE, firstReporter, LOG_PRIVATE);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM tb_v2_content_report WHERE id BETWEEN ? AND ?", REPORT_FIRST, REPORT_PRIVATE);
        jdbc.update("DELETE FROM tb_v2_ramen_log_image WHERE ramen_log_id BETWEEN ? AND ?", LOG_PUBLIC, LOG_OTHER);
        jdbc.update("DELETE FROM tb_v2_ramen_log WHERE id BETWEEN ? AND ?", LOG_PUBLIC, LOG_OTHER);
        jdbc.update("DELETE FROM tb_v2_shop WHERE id = ?", SHOP);
        created.forEach(users::deleteById);
    }

    @Test
    void 목록은_삭제된_기록을_빼고_비공개_기록은_작성자와_작성일만_보여준다() throws Exception {
        ResultActions all = mvc.perform(byAuthor())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.page.totalElements").value(2))
                .andExpect(jsonPath("$.data.items[0].id").value(String.valueOf(LOG_PRIVATE)))
                .andExpect(jsonPath("$.data.items[0].visibility").value("PRIVATE"))
                .andExpect(jsonPath("$.data.items[0].createdAt").isNotEmpty())
                .andExpect(jsonPath("$.data.items[0].author.id").value(String.valueOf(author)))
                .andExpect(jsonPath("$.data.items[0].author.nickname").value(startsWith("작성자")))
                .andExpect(jsonPath("$.data.items[0].deletedAt").doesNotExist())
                .andExpect(jsonPath("$.data.items[1].id").value(String.valueOf(LOG_PUBLIC)))
                .andExpect(jsonPath("$.data.items[1].visibility").value("PUBLIC"))
                .andExpect(jsonPath("$.data.items[1].shop.id").value(String.valueOf(SHOP)))
                .andExpect(jsonPath("$.data.items[1].shop.name").value("기록관리 매장"))
                .andExpect(jsonPath("$.data.items[1].shop.branchName").value("본점"))
                .andExpect(jsonPath("$.data.items[1].menuName").value("특제 쇼유"))
                .andExpect(jsonPath("$.data.items[1].ramenType").value("쇼유"))
                .andExpect(jsonPath("$.data.items[1].visitedAt").value("2026-09-01"))
                .andExpect(jsonPath("$.data.items[1].note").value("맑은 국물"))
                .andExpect(jsonPath("$.data.items[1].thumbnailUrl").value("https://images.raota.net/first.jpg"))
                .andExpect(jsonPath("$.data.items[1].imageCount").value(2))
                .andExpect(jsonPath("$.data.items[1].likeCount").value(0))
                .andExpect(jsonPath("$.data.items[1].commentCount").value(0))
                .andExpect(jsonPath("$.data.items[1].openReportCount").value(2))
                .andExpect(jsonPath("$.data.items[1].updatedAt").isNotEmpty());
        expectHidden(all, "$.data.items[0]", ROW_HIDDEN_FIELDS);

        mvc.perform(byAuthor().param("visibility", "PRIVATE"))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(String.valueOf(LOG_PRIVATE)));
        mvc.perform(get(BASE).param("userId", String.valueOf(other)).with(ADMIN))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(String.valueOf(LOG_OTHER)));
        mvc.perform(byAuthor().param("size", "1"))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.page.totalPages").value(2))
                .andExpect(jsonPath("$.data.page.hasNext").value(true));
        mvc.perform(byAuthor().param("keyword", "가".repeat(101))).andExpect(status().isBadRequest());
    }

    @Test
    void 키워드와_매장_검색은_공개_기록만_찾는다() throws Exception {
        mvc.perform(byAuthor().param("keyword", "  비밀 "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0));
        mvc.perform(byAuthor().param("keyword", "지운"))
                .andExpect(jsonPath("$.data.items.length()").value(0));
        mvc.perform(byAuthor().param("keyword", "맑은"))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(String.valueOf(LOG_PUBLIC)));
        mvc.perform(get(BASE).param("shopId", String.valueOf(SHOP)).with(ADMIN))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(String.valueOf(LOG_PUBLIC)));
        mvc.perform(get(BASE)
                        .param("shopId", String.valueOf(SHOP))
                        .param("visibility", "PRIVATE")
                        .with(ADMIN))
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    void 상세는_공개_기록만_내용을_보여주고_삭제된_기록은_없는_기록으로_본다() throws Exception {
        mvc.perform(get(BASE + "/" + LOG_PUBLIC).with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visibility").value("PUBLIC"))
                .andExpect(jsonPath("$.data.shop.name").value("기록관리 매장"))
                .andExpect(jsonPath("$.data.openReportCount").value(2))
                .andExpect(jsonPath("$.data.imageCount").value(2))
                .andExpect(jsonPath("$.data.imageUrls[0]").value("https://images.raota.net/first.jpg"))
                .andExpect(jsonPath("$.data.imageUrls[1]").value("https://images.raota.net/second.jpg"))
                .andExpect(jsonPath("$.data.tasteNotes.length()").value(2))
                .andExpect(jsonPath("$.data.tasteNotes[0].code").value("NOODLE_02"))
                .andExpect(jsonPath("$.data.tasteNotes[0].category").value("NOODLE"))
                .andExpect(jsonPath("$.data.tasteNotes[0].label").value("단단해요"))
                .andExpect(jsonPath("$.data.tasteNotes[1].code").value("RETIRED_01"))
                .andExpect(jsonPath("$.data.tasteNotes[1].category").isEmpty())
                .andExpect(jsonPath("$.data.tasteNotes[1].label").isEmpty())
                .andExpect(jsonPath("$.data.revisitIntention").value("OFTEN"))
                .andExpect(jsonPath("$.data.scores.satisfaction").value(4))
                .andExpect(jsonPath("$.data.scores.brothDensity").value(5))
                .andExpect(jsonPath("$.data.scores.noodleFirmness").value(3))
                .andExpect(jsonPath("$.data.scores.topping").value(2));
        ResultActions hidden = mvc.perform(get(BASE + "/" + LOG_PRIVATE).with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(String.valueOf(LOG_PRIVATE)))
                .andExpect(jsonPath("$.data.visibility").value("PRIVATE"))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.data.author.nickname").value(startsWith("작성자")));
        expectHidden(hidden, "$.data", ROW_HIDDEN_FIELDS);
        expectHidden(hidden, "$.data", DETAIL_HIDDEN_FIELDS);
        for (long missing : new long[] {LOG_DELETED, 900006899L}) {
            mvc.perform(get(BASE + "/" + missing).with(ADMIN))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
        }
    }

    @Test
    void 공개였던_기록을_비공개로_바꾸면_바로_가리고_삭제를_거절한다() throws Exception {
        jdbc.update("UPDATE tb_v2_ramen_log SET visibility = 'PRIVATE' WHERE id = ?", LOG_PUBLIC);

        ResultActions listed = mvc.perform(byAuthor())
                .andExpect(jsonPath("$.data.items[1].id").value(String.valueOf(LOG_PUBLIC)))
                .andExpect(jsonPath("$.data.items[1].visibility").value("PRIVATE"));
        expectHidden(listed, "$.data.items[1]", ROW_HIDDEN_FIELDS);
        ResultActions detail = mvc.perform(get(BASE + "/" + LOG_PUBLIC).with(ADMIN));
        expectHidden(detail, "$.data", ROW_HIDDEN_FIELDS);
        expectHidden(detail, "$.data", DETAIL_HIDDEN_FIELDS);
        mvc.perform(get(BASE).param("shopId", String.valueOf(SHOP)).with(ADMIN))
                .andExpect(jsonPath("$.data.items.length()").value(0));
        mvc.perform(delete(BASE + "/" + LOG_PUBLIC).with(ADMIN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        assertThat(jdbc.queryForObject(
                        "SELECT deleted_at IS NULL FROM tb_v2_ramen_log WHERE id = ?", Boolean.class, LOG_PUBLIC))
                .isTrue();
    }

    @Test
    void 공개_기록을_삭제하면_열린_신고를_닫고_집계를_줄이며_비공개와_삭제된_기록은_거절한다() throws Exception {
        mvc.perform(delete(BASE + "/" + LOG_PUBLIC).with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.closedReportCount").value(2));

        assertThat(jdbc.queryForObject(
                        "SELECT deleted_at IS NOT NULL FROM tb_v2_ramen_log WHERE id = ?", Boolean.class, LOG_PUBLIC))
                .isTrue();
        assertThat(jdbc.queryForObject("SELECT log_count FROM tb_v2_user WHERE id = ?", Integer.class, author))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT log_count FROM tb_v2_shop WHERE id = ?", Integer.class, SHOP))
                .isEqualTo(2);
        assertThat(reportStatuses()).containsExactly("RESOLVED", "RESOLVED", "OPEN");

        for (long gone : new long[] {LOG_PUBLIC, LOG_DELETED, 900006899L}) {
            mvc.perform(delete(BASE + "/" + gone).with(ADMIN))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
        }
        mvc.perform(delete(BASE + "/" + LOG_PRIVATE).with(ADMIN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        assertThat(jdbc.queryForObject(
                        "SELECT deleted_at IS NULL FROM tb_v2_ramen_log WHERE id = ?", Boolean.class, LOG_PRIVATE))
                .isTrue();
        assertThat(jdbc.queryForObject("SELECT log_count FROM tb_v2_user WHERE id = ?", Integer.class, author))
                .isEqualTo(1);
        assertThat(reportStatuses()).containsExactly("RESOLVED", "RESOLVED", "OPEN");
    }

    @Test
    void 회원별_공개_비공개_기록_수는_삭제된_기록을_빼고_센다() throws Exception {
        mvc.perform(counts(author))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(String.valueOf(author)))
                .andExpect(jsonPath("$.data.publicCount").value(1))
                .andExpect(jsonPath("$.data.privateCount").value(1));
        mvc.perform(counts(other))
                .andExpect(jsonPath("$.data.publicCount").value(0))
                .andExpect(jsonPath("$.data.privateCount").value(1));
        mvc.perform(counts(900006898L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publicCount").value(0))
                .andExpect(jsonPath("$.data.privateCount").value(0));
        mvc.perform(get(BASE + "/counts").with(ADMIN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 웹_관리자만_쓸_수_있다() throws Exception {
        RequestPostProcessor member = user("member").roles("USER");
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE).with(member)).andExpect(status().isForbidden());
        mvc.perform(get(BASE + "/" + LOG_PUBLIC).with(member)).andExpect(status().isForbidden());
        mvc.perform(get(BASE + "/counts")
                        .param("userId", String.valueOf(author))
                        .with(member))
                .andExpect(status().isForbidden());
        mvc.perform(delete(BASE + "/" + LOG_PUBLIC).with(member)).andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject(
                        "SELECT deleted_at IS NULL FROM tb_v2_ramen_log WHERE id = ?", Boolean.class, LOG_PUBLIC))
                .isTrue();
    }

    private MockHttpServletRequestBuilder byAuthor() {
        return get(BASE).param("userId", String.valueOf(author)).with(ADMIN);
    }

    private MockHttpServletRequestBuilder counts(Long userId) {
        return get(BASE + "/counts").param("userId", String.valueOf(userId)).with(ADMIN);
    }

    private static void expectHidden(ResultActions actions, String path, List<String> fields) throws Exception {
        for (String field : fields) {
            actions.andExpect(jsonPath(path + "." + field).value(nullValue()));
        }
    }

    private List<String> reportStatuses() {
        return jdbc.queryForList(
                "SELECT status FROM tb_v2_content_report WHERE id BETWEEN ? AND ? ORDER BY id",
                String.class,
                REPORT_FIRST,
                REPORT_PRIVATE);
    }

    private void scoredLog(
            long id, Long userId, String menuName, String note, String visibility, String createdAt, String deletedAt) {
        jdbc.update("""
                INSERT INTO tb_v2_ramen_log (id, user_id, shop_id, visited_at, menu_name, ramen_type,
                    satisfaction_score, broth_density_score, noodle_firmness_score, topping_score,
                    revisit_intention, note, taste_note_codes, visibility, created_at, updated_at, deleted_at)
                VALUES (?, ?, ?, '2026-09-01', ?, '쇼유', 4, 5, 3, 2, 'OFTEN', ?, '["NOODLE_02", "RETIRED_01"]', ?, ?,
                    ?, ?)
                """, id, userId, SHOP, menuName, note, visibility, createdAt, createdAt, deletedAt);
    }

    private void image(long logId, String url, int sortOrder) {
        jdbc.update(
                "INSERT INTO tb_v2_ramen_log_image (ramen_log_id, url, sort_order) VALUES (?, ?, ?)",
                logId,
                url,
                sortOrder);
    }

    private void report(long id, Long reporterId, long logId) {
        jdbc.update("""
                INSERT INTO tb_v2_content_report (id, reporter_id, target_type, target_id, reason, status, created_at)
                VALUES (?, ?, 'RAMEN_LOG', ?, 'SPAM', 'OPEN', '2026-09-06 00:00:00')
                """, id, reporterId, logId);
    }

    private Long active(String nickname) {
        MobileUser user = MobileUser.onboarding("ramen-log-admin-" + UUID.randomUUID() + "@example.com");
        user.completeOnboarding(
                Nickname.of(nickname + UUID.randomUUID().toString().substring(0, 4)), Instant.now());
        Long id = users.saveAndFlush(user).getId();
        created.add(id);
        return id;
    }
}
