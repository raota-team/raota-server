package com.raota.mobile.ramenlog.infrastructure.messaging;

import com.raota.mobile.account.domain.event.MobileMemberPurgedEvent;
import com.raota.mobile.shop.application.facade.MobileShopRamenLogFacade;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 회원 익명화 트랜잭션에서 사진 행과 기록을 지우고 활성 기록의 매장 집계를 되돌린다. */
@Component
@RequiredArgsConstructor
public class MobileRamenLogPurgeListener {

    private final JdbcTemplate jdbc;

    private final MobileShopRamenLogFacade shops;

    @EventListener
    public void onMemberPurged(MobileMemberPurgedEvent event) {
        List<CountedLog> counted = jdbc.query("""
                SELECT shop_id, satisfaction_score FROM tb_v2_ramen_log
                WHERE user_id = ? AND deleted_at IS NULL
                """, (rs, row) -> {
            long shopId = rs.getLong("shop_id");
            int score = rs.getInt("satisfaction_score");
            return new CountedLog(shopId, rs.wasNull() ? null : score);
        }, event.userId());
        jdbc.update("""
                DELETE FROM tb_v2_ramen_log_image
                WHERE ramen_log_id IN (SELECT id FROM tb_v2_ramen_log WHERE user_id = ?)
                """, event.userId());
        jdbc.update("DELETE FROM tb_v2_ramen_log WHERE user_id = ?", event.userId());
        for (CountedLog log : counted) {
            shops.recordLogRemoved(log.shopId(), log.satisfaction());
        }
    }

    private record CountedLog(long shopId, Integer satisfaction) {
    }

}
