package com.raota.agent.application.mobile.result;

import com.raota.mobile.shop.application.result.MobileShopSummary;
import java.util.List;
import java.util.Map;

/**
 * 앱의 {@code RecommendationResponse}와 같은 형태다. 매장은 최대 6곳이다. {@code recommendationId}는 노출
 * 기록의 ID이며, 기록을 남기지 못했으면 {@code null}이다. {@code reasons}는 매장 ID마다 LLM이 쓴 추천 이유(2~3문장, 한
 * 줄)이고, 이유를 만들지 못한 매장은 빠진다(전부 실패하면 빈 객체).
 */
public record MobileRecommendationResult(String recommendationId, List<MobileShopSummary> shops,
        List<String> appliedConditions, Map<String, String> reasons) {
}
