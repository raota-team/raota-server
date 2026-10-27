package com.raota.mobile.ramenlog.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.raota.mobile.account.application.service.MobileWithdrawalPurgeService;
import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.model.MobileUserStatus;
import com.raota.mobile.account.domain.model.Nickname;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import com.raota.support.BaseIntegrationTest;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class MobileRamenLogPurgeIntegrationTest extends BaseIntegrationTest {

    private static final long SHOP = 900004851L;

    private static final long DUE_LOG = 900004852L;

    private static final long DELETED_LOG = 900004853L;

    private static final long OTHER_LOG = 900004854L;

    private static final long DUE_UNSCORED_LOG = 900004855L;

    private static final Instant REQUESTED_AT = Instant.parse("2026-09-01T00:00:00Z");

    @Autowired
    private MobileWithdrawalPurgeService purges;

    @Autowired
    private MobileUserRepository users;

    @Autowired
    private JdbcTemplate jdbc;

    private Long dueId;

    private Long otherId;

    @BeforeEach
    void setUp() {
        MobileUser due = MobileUser.onboarding("purge-ramen@example.com");
        due.completeOnboarding(Nickname.of("PurgeRamen"), REQUESTED_AT);
        due.requestWithdrawal(REQUESTED_AT, Duration.ofDays(30));
        dueId = users.saveAndFlush(due).getId();
        MobileUser other = MobileUser.onboarding("remaining-ramen@example.com");
        other.completeOnboarding(Nickname.of("RemainRamen"), REQUESTED_AT);
        otherId = users.saveAndFlush(other).getId();
        jdbc.update("UPDATE tb_v2_user SET log_count = 3 WHERE id = ?", dueId);
        jdbc.update("UPDATE tb_v2_user SET log_count = 1 WHERE id = ?", otherId);
        jdbc.update("""
                INSERT INTO tb_v2_shop (id, name, address, ramen_types, tags, business_status, ai_summary_keywords,
                    log_count, satisfaction_score_sum, scored_log_count, is_published, created_at, updated_at)
                VALUES (?, '정리 매장', '서울', '[]', '[]', 'OPERATIONAL', '[]', 3, 9, 2, TRUE,
                    '2026-09-01 00:00:00', '2026-09-01 00:00:00')
                """, SHOP);
        insertLog(DUE_LOG, dueId, 5, false);
        insertLog(DELETED_LOG, dueId, 3, true);
        insertLog(OTHER_LOG, otherId, 4, false);
        insertLog(DUE_UNSCORED_LOG, dueId, null, false);
        for (long id : new long[] {DUE_LOG, DELETED_LOG, OTHER_LOG, DUE_UNSCORED_LOG}) {
            jdbc.update(
                    "INSERT INTO tb_v2_ramen_log_image (ramen_log_id, url, sort_order) VALUES (?, ?, 0)",
                    id,
                    "https://mock.cdn.com/v2/ramen-logs/" + id + ".png");
        }
        jdbc.update("UPDATE tb_v2_ramen_log SET like_count = 2 WHERE id = ?", OTHER_LOG);
        jdbc.update("UPDATE tb_v2_ramen_log SET like_count = 1 WHERE id = ?", DUE_LOG);
        for (Object[] like : List.of(
                new Object[] {dueId, OTHER_LOG}, new Object[] {otherId, OTHER_LOG}, new Object[] {otherId, DUE_LOG})) {
            jdbc.update("""
                    INSERT INTO tb_v2_ramen_log_like (user_id, ramen_log_id, created_at)
                    VALUES (?, ?, '2026-09-01 00:00:00')
                    """, like);
        }
        jdbc.update("UPDATE tb_v2_ramen_log SET comment_count = 2 WHERE id = ?", OTHER_LOG);
        jdbc.update("UPDATE tb_v2_ramen_log SET comment_count = 1 WHERE id = ?", DUE_LOG);
        for (Object[] comment : List.of(
                new Object[] {OTHER_LOG, dueId, null},
                new Object[] {OTHER_LOG, otherId, null},
                new Object[] {OTHER_LOG, dueId, "2026-09-02 00:00:00"},
                new Object[] {DUE_LOG, otherId, null})) {
            jdbc.update("""
                    INSERT INTO tb_v2_ramen_log_comment (ramen_log_id, user_id, content, created_at, updated_at,
                        deleted_at)
                    VALUES (?, ?, '댓글', '2026-09-01 00:00:00', '2026-09-01 00:00:00', ?)
                    """, comment);
        }
        jdbc.update("""
                INSERT INTO tb_v2_content_report (reporter_id, target_type, target_id, reason, status, created_at)
                VALUES (?, 'RAMEN_LOG', ?, 'SPAM', 'OPEN', '2026-09-01 00:00:00'),
                    (?, 'RAMEN_LOG', ?, 'SPAM', 'OPEN', '2026-09-01 00:00:00'),
                    (?, 'RAMEN_LOG', ?, 'ABUSE', 'OPEN', '2026-09-01 00:00:00')
                """, dueId, OTHER_LOG, otherId, DUE_LOG, otherId, OTHER_LOG);
        jdbc.update("""
                INSERT INTO tb_v2_content_report (reporter_id, target_type, target_id, reason, status, created_at)
                SELECT ?, 'RAMEN_LOG_COMMENT', id, 'SPAM', 'OPEN', '2026-09-01 00:00:00'
                FROM tb_v2_ramen_log_comment WHERE ramen_log_id = ? AND user_id = ? AND deleted_at IS NULL
                """, otherId, OTHER_LOG, dueId);
        jdbc.update("""
                INSERT INTO tb_v2_user_block (blocker_id, blocked_id, created_at)
                VALUES (?, ?, '2026-09-01 00:00:00'), (?, ?, '2026-09-01 00:00:00')
                """, dueId, otherId, otherId, dueId);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM tb_v2_content_report WHERE reporter_id IN (?, ?)", dueId, otherId);
        jdbc.update("DELETE FROM tb_v2_user_block WHERE blocker_id IN (?, ?)", dueId, otherId);
        jdbc.update("DELETE FROM tb_v2_ramen_log_comment WHERE ramen_log_id IN (?, ?)", DUE_LOG, OTHER_LOG);
        jdbc.update("DELETE FROM tb_v2_ramen_log_like WHERE ramen_log_id IN (?, ?)", DUE_LOG, OTHER_LOG);
        jdbc.update(
                "DELETE FROM tb_v2_ramen_log_image WHERE ramen_log_id IN (?, ?, ?, ?)",
                DUE_LOG,
                DELETED_LOG,
                OTHER_LOG,
                DUE_UNSCORED_LOG);
        jdbc.update(
                "DELETE FROM tb_v2_ramen_log WHERE id IN (?, ?, ?, ?)",
                DUE_LOG,
                DELETED_LOG,
                OTHER_LOG,
                DUE_UNSCORED_LOG);
        jdbc.update("DELETE FROM tb_v2_shop WHERE id = ?", SHOP);
        users.deleteById(dueId);
        users.deleteById(otherId);
    }

    @Test
    void 유예_기한이_지난_회원의_신고와_차단과_공감과_댓글과_사진과_기록만_삭제하고_공감_수와_댓글_수와_활성_기록의_매장_집계를_되돌린다() {
        assertThat(purges.purgeExpired(REQUESTED_AT.plus(Duration.ofDays(30)))).isEqualTo(1);
        assertThat(users.findById(dueId).orElseThrow().getStatus()).isEqualTo(MobileUserStatus.WITHDRAWN);
        assertThat(jdbc.queryForObject("SELECT log_count FROM tb_v2_user WHERE id = ?", Integer.class, dueId))
                .isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tb_v2_ramen_log WHERE user_id = ?", Integer.class, dueId))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM tb_v2_ramen_log_image WHERE ramen_log_id IN (?, ?, ?)",
                        Integer.class,
                        DUE_LOG,
                        DELETED_LOG,
                        DUE_UNSCORED_LOG))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM tb_v2_ramen_log WHERE user_id = ?", Integer.class, otherId))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM tb_v2_ramen_log_image WHERE ramen_log_id = ?", Integer.class, OTHER_LOG))
                .isEqualTo(1);
        assertThat(jdbc.queryForMap(
                        "SELECT log_count, satisfaction_score_sum, scored_log_count FROM tb_v2_shop WHERE id = ?",
                        SHOP))
                .containsEntry("log_count", 1)
                .containsEntry("satisfaction_score_sum", 4)
                .containsEntry("scored_log_count", 1);
        assertThat(jdbc.queryForObject("SELECT like_count FROM tb_v2_ramen_log WHERE id = ?", Integer.class, OTHER_LOG))
                .isEqualTo(1);
        assertThat(jdbc.queryForList(
                        "SELECT user_id FROM tb_v2_ramen_log_like WHERE ramen_log_id IN (?, ?)",
                        Long.class,
                        DUE_LOG,
                        OTHER_LOG))
                .containsExactly(otherId);
        assertThat(jdbc.queryForObject(
                        "SELECT comment_count FROM tb_v2_ramen_log WHERE id = ?", Integer.class, OTHER_LOG))
                .isEqualTo(1);
        assertThat(jdbc.queryForList(
                        "SELECT user_id FROM tb_v2_ramen_log_comment WHERE ramen_log_id IN (?, ?)",
                        Long.class,
                        DUE_LOG,
                        OTHER_LOG))
                .containsExactly(otherId);
        assertThat(jdbc.queryForList("""
                SELECT CONCAT(reporter_id, ':', target_type, ':', target_id) FROM tb_v2_content_report
                WHERE reporter_id IN (?, ?)
                """, String.class, dueId, otherId))
                .containsExactly(otherId + ":RAMEN_LOG:" + OTHER_LOG);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM tb_v2_user_block WHERE blocker_id IN (?, ?)",
                        Integer.class,
                        dueId,
                        otherId))
                .isZero();
    }

    private void insertLog(long id, Long userId, Integer satisfaction, boolean deleted) {
        Integer otherScore = null;
        if (satisfaction != null) {
            otherScore = 3;
        }
        java.sql.Timestamp deletedAt = null;
        if (deleted) {
            deletedAt = java.sql.Timestamp.from(REQUESTED_AT);
        }
        jdbc.update("""
                INSERT INTO tb_v2_ramen_log (id, user_id, shop_id, visited_at, menu_name, ramen_type,
                    satisfaction_score, broth_density_score, noodle_firmness_score, topping_score,
                    revisit_intention, note, taste_note_codes, visibility, created_at, updated_at, deleted_at)
                VALUES (?, ?, ?, '2026-09-01', '라멘', '쇼유', ?, ?, ?, ?,
                    'OFTEN', '', '[]', 'PUBLIC', '2026-09-01 00:00:00', '2026-09-01 00:00:00', ?)
                """, id, userId, SHOP, satisfaction, otherScore, otherScore, otherScore, deletedAt);
    }
}
