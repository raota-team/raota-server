package com.raota.mobile.shop.application;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** 회원 익명화 트랜잭션에서 해당 회원의 북마크만 지운다. */
class MobileShopPurgeIntegrationTest extends BaseIntegrationTest {

    private static final long SHARED = 900004101L;

    private static final long INCONSISTENT_COUNT = 900004102L;

    private static final Instant REQUESTED_AT = Instant.parse("2026-09-01T00:00:00Z");

    @Autowired
    private MobileWithdrawalPurgeService purges;

    @Autowired
    private MobileUserRepository users;

    @Autowired
    private JdbcTemplate jdbc;

    private Long dueId;

    private Long activeId;

    @AfterEach
    void cleanUp() {
        if (dueId != null && activeId != null) {
            jdbc.update("DELETE FROM tb_v2_shop_bookmark WHERE shop_id IN (?, ?)", SHARED, INCONSISTENT_COUNT);
            jdbc.update("DELETE FROM tb_v2_shop WHERE id IN (?, ?)", SHARED, INCONSISTENT_COUNT);
            users.deleteById(dueId);
            users.deleteById(activeId);
        }
    }

    @Test
    void 기한이_지난_회원만_정리하고_다른_회원의_북마크와_카운트는_유지한다() {
        MobileUser due = MobileUser.onboarding("due@example.com");
        due.requestWithdrawal(REQUESTED_AT, Duration.ofDays(30));
        dueId = users.saveAndFlush(due).getId();
        MobileUser active = MobileUser.onboarding("active@example.com");
        active.completeOnboarding(Nickname.of("PurgeMember"), REQUESTED_AT);
        activeId = users.saveAndFlush(active).getId();
        for (long shopId : List.of(SHARED, INCONSISTENT_COUNT)) {
            jdbc.update(
                    """
                            INSERT INTO tb_v2_shop (id, name, address, ramen_types, tags, business_status,
                                ai_summary_keywords, view_count, log_count, bookmark_count, is_published, created_at, updated_at)
                            VALUES (?, '정리 매장', '서울', '[]', '[]', 'UNKNOWN', '[]', 0, 0, ?, TRUE,
                                '2026-09-01 00:00:00.000000', '2026-09-01 00:00:00.000000')
                            """,
                    shopId, shopId == SHARED ? 2 : 0);
        }
        jdbc.update(
                "INSERT INTO tb_v2_shop_bookmark (user_id, shop_id, created_at) VALUES (?, ?, CURRENT_TIMESTAMP(6))",
                dueId, SHARED);
        jdbc.update(
                "INSERT INTO tb_v2_shop_bookmark (user_id, shop_id, created_at) VALUES (?, ?, CURRENT_TIMESTAMP(6))",
                dueId, INCONSISTENT_COUNT);
        jdbc.update(
                "INSERT INTO tb_v2_shop_bookmark (user_id, shop_id, created_at) VALUES (?, ?, CURRENT_TIMESTAMP(6))",
                activeId, SHARED);

        assertThat(purges.purgeExpired(REQUESTED_AT.plus(Duration.ofDays(30)))).isEqualTo(1);
        assertThat(users.findById(dueId).orElseThrow().getStatus()).isEqualTo(MobileUserStatus.WITHDRAWN);
        assertThat(
                jdbc.queryForObject("SELECT COUNT(*) FROM tb_v2_shop_bookmark WHERE user_id = ?", Integer.class, dueId))
            .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tb_v2_shop_bookmark WHERE user_id = ?", Integer.class,
                activeId))
            .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT bookmark_count FROM tb_v2_shop WHERE id = ?", Integer.class, SHARED))
            .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT bookmark_count FROM tb_v2_shop WHERE id = ?", Integer.class,
                INCONSISTENT_COUNT))
            .isZero();
    }

}
