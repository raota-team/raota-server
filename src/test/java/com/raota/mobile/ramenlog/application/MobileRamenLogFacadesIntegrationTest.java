package com.raota.mobile.ramenlog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.raota.mobile.account.application.facade.MobileAccountRamenLogFacade;
import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.model.Nickname;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import com.raota.mobile.shop.application.facade.MobileShopRamenLogFacade;
import com.raota.support.BaseIntegrationTest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class MobileRamenLogFacadesIntegrationTest extends BaseIntegrationTest {

    private static final long SHOP_ID = 900004621L;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MobileShopRamenLogFacade shops;

    @Autowired
    private MobileAccountRamenLogFacade accounts;

    @Autowired
    private MobileUserRepository users;

    private Long userId;

    @BeforeEach
    void setUp() {
        jdbc.update("""
                INSERT INTO tb_v2_shop (id, name, address, region, ramen_types, tags, business_status,
                    ai_summary_keywords, is_published, created_at, updated_at)
                VALUES (?, '기록 매장', '서울', '서울', '[]', '[]', 'OPERATIONAL', '[]', TRUE,
                    '2026-09-01 00:00:00', '2026-09-01 00:00:00')
                """, SHOP_ID);
        MobileUser user = MobileUser.onboarding("facade-test@example.com");
        user.completeOnboarding(Nickname.of("Log" + UUID.randomUUID().toString().substring(0, 8)), Instant.now());
        userId = users.saveAndFlush(user).getId();
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM tb_v2_shop WHERE id = ?", SHOP_ID);
        users.deleteById(userId);
    }

    @Test
    void 공개_매장과_작성자를_묶어서_읽고_비공개_매장은_제외한다() {
        assertThat(shops.findPublishedShopRef(SHOP_ID).name()).isEqualTo("기록 매장");
        assertThat(accounts.authors(List.of(userId, userId + 1))).containsOnlyKeys(userId);
        assertThat(accounts.authors(List.of(userId)).get(userId).active()).isTrue();
        jdbc.update("UPDATE tb_v2_shop SET is_published = FALSE WHERE id = ?", SHOP_ID);
        assertThatThrownBy(() -> shops.findPublishedShopRef(SHOP_ID)).isInstanceOfSatisfying(MobileException.class,
                exception -> assertThat(exception.code()).isEqualTo(MobileErrorCode.RESOURCE_NOT_FOUND));
        assertThat(shops.findShopRefs(List.of(SHOP_ID)).get(SHOP_ID).name()).isEqualTo("기록 매장");
    }

    @Test
    void 동시_기록_추가가_수를_잃지_않으며_삭제_및_만족도_변경이_영_아래로_가지_않는다() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var add = (java.util.concurrent.Callable<Void>) () -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new AssertionError("시작 신호가 없습니다.");
                }
                for (int index = 0; index < 10; index++) {
                    shops.recordLogAdded(SHOP_ID, 5);
                    accounts.incrementLogCount(userId);
                }
                return null;
            };
            var first = pool.submit(add);
            var second = pool.submit(add);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            first.get(20, TimeUnit.SECONDS);
            second.get(20, TimeUnit.SECONDS);
        }
        assertThat(jdbc.queryForMap(
                "SELECT log_count, satisfaction_score_sum, scored_log_count FROM tb_v2_shop WHERE id = ?", SHOP_ID))
            .containsEntry("log_count", 20)
            .containsEntry("satisfaction_score_sum", 100)
            .containsEntry("scored_log_count", 20);
        assertThat(accounts.authors(List.of(userId)).get(userId).logCount()).isEqualTo(20);

        shops.recordSatisfactionChanged(SHOP_ID, 5, 3);
        assertThat(jdbc.queryForObject("SELECT satisfaction_score_sum FROM tb_v2_shop WHERE id = ?", Integer.class,
                SHOP_ID))
            .isEqualTo(98);
        shops.recordLogRemoved(SHOP_ID, 3);
        for (int index = 0; index < 21; index++) {
            shops.recordLogRemoved(SHOP_ID, 5);
            accounts.decrementLogCount(userId);
        }
        assertThat(jdbc.queryForMap(
                "SELECT log_count, satisfaction_score_sum, scored_log_count FROM tb_v2_shop WHERE id = ?", SHOP_ID))
            .containsEntry("log_count", 0)
            .containsEntry("satisfaction_score_sum", 0)
            .containsEntry("scored_log_count", 0);
        assertThat(accounts.authors(List.of(userId)).get(userId).logCount()).isZero();
    }

}
