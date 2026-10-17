package com.raota.agent.application.mobile.result;

import com.raota.mobile.shop.application.result.MobileShopSummary;
import java.util.List;

/**
 * 앱의 {@code RecommendationResponse}와 같은 형태다. 매장은 최대 6곳이다. {@code recommendationId}는 노출
 * 기록의 ID이며, 기록을 남기지 못했으면 {@code null}이다.
 */
public record MobileRecommendationResult(String recommendationId, List<MobileShopSummary> shops,
        List<String> appliedConditions) {
}
