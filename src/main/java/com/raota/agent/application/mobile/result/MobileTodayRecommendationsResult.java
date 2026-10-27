package com.raota.agent.application.mobile.result;

import com.raota.mobile.shop.application.result.MobileShopSummaryResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 앱의 "오늘 뽑은 식권"이다. 하루 {@code dailyLimit}장 중 {@code remaining}장이 남았고, {@code tickets}는 오늘
 * 뽑은 식권을 최신순으로 담는다. 식권의 매장은 지금도 추천해도 되는 매장만 남긴다.
 */
public record MobileTodayRecommendationsResult(int dailyLimit, int remaining, List<Ticket> tickets) {

    public record Ticket(
            String recommendationId,
            Instant createdAt,
            List<MobileShopSummaryResult> shops,
            List<String> appliedConditions,
            Map<String, String> reasons) {}
}
