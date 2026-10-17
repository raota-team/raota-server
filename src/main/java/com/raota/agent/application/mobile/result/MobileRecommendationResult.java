package com.raota.agent.application.mobile.result;

import com.raota.mobile.shop.application.result.MobileShopSummary;
import java.util.List;

/** 앱의 {@code RecommendationResponse}와 같은 형태다. 매장은 최대 6곳이다. */
public record MobileRecommendationResult(List<MobileShopSummary> shops, List<String> appliedConditions) {
}
