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
        jdbc.update("UPDATE tb_v2_user SET log_count = 2 WHERE id = ?", dueId);
        jdbc.update("UPDATE tb_v2_user SET log_count = 1 WHERE id = ?", otherId);
        jdbc.update("""
                INSERT INTO tb_v2_shop (id, name, address, ramen_types, tags, business_status, ai_summary_keywords,
                    log_count, satisfaction_score_sum, scored_log_count, is_published, created_at, updated_at)
                VALUES (?, '정리 매장', '서울', '[]', '[]', 'OPERATIONAL', '[]', 2, 9, 2, TRUE,
                    '2026-09-01 00:00:00', '2026-09-01 00:00:00')
                """, SHOP);
        insertLog(DUE_LOG, dueId, 5, false);
        insertLog(DELETED_LOG, dueId, 3, true);
        insertLog(OTHER_LOG, otherId, 4, false);
        for (long id : new long[] { DUE_LOG, DELETED_LOG, OTHER_LOG }) {
            jdbc.update("INSERT INTO tb_v2_ramen_log_image (ramen_log_id, url, sort_order) VALUES (?, ?, 0)", id,
                    "https://mock.cdn.com/v2/ramen-logs/" + id + ".png");
        }
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM tb_v2_ramen_log_image WHERE ramen_log_id IN (?, ?, ?)", DUE_LOG, DELETED_LOG,
                OTHER_LOG);
        jdbc.update("DELETE FROM tb_v2_ramen_log WHERE id IN (?, ?, ?)", DUE_LOG, DELETED_LOG, OTHER_LOG);
        jdbc.update("DELETE FROM tb_v2_shop WHERE id = ?", SHOP);
        users.deleteById(dueId);
        users.deleteById(otherId);
    }

    @Test
    void 유예_기한에_지난_회원의_사진과_기록만_삭제하고_활성_기록의_매장_집계만_되돌린다() {
        assertThat(purges.purgeExpired(REQUESTED_AT.plus(Duration.ofDays(30)))).isEqualTo(1);
        assertThat(users.findById(dueId).orElseThrow().getStatus()).isEqualTo(MobileUserStatus.WITHDRAWN);
        assertThat(jdbc.queryForObject("SELECT log_count FROM tb_v2_user WHERE id = ?", Integer.class, dueId))
            .isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tb_v2_ramen_log WHERE user_id = ?", Integer.class, dueId))
            .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tb_v2_ramen_log_image WHERE ramen_log_id IN (?, ?)",
                Integer.class, DUE_LOG, DELETED_LOG))
            .isZero();
        assertThat(
                jdbc.queryForObject("SELECT COUNT(*) FROM tb_v2_ramen_log WHERE user_id = ?", Integer.class, otherId))
            .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tb_v2_ramen_log_image WHERE ramen_log_id = ?",
                Integer.class, OTHER_LOG))
            .isEqualTo(1);
        assertThat(jdbc.queryForMap(
                "SELECT log_count, satisfaction_score_sum, scored_log_count FROM tb_v2_shop WHERE id = ?", SHOP))
            .containsEntry("log_count", 1)
            .containsEntry("satisfaction_score_sum", 4)
            .containsEntry("scored_log_count", 1);
    }

    private void insertLog(long id, Long userId, int satisfaction, boolean deleted) {
        jdbc.update("""
                INSERT INTO tb_v2_ramen_log (id, user_id, shop_id, visited_at, menu_name, ramen_type,
                    satisfaction_score, broth_density_score, noodle_firmness_score, topping_score,
                    revisit_intention, note, taste_note_codes, visibility, created_at, updated_at, deleted_at)
                VALUES (?, ?, ?, '2026-09-01', '라멘', '쇼유', ?, 3, 3, 3,
                    'OFTEN', '', '[]', 'PUBLIC', '2026-09-01 00:00:00', '2026-09-01 00:00:00', ?)
                """, id, userId, SHOP, satisfaction, deleted ? java.sql.Timestamp.from(REQUESTED_AT) : null);
    }

}
