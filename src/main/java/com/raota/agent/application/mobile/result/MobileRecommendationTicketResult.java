package com.raota.agent.application.mobile.result;

import com.raota.mobile.shop.application.result.MobileShopSummaryResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 앱 AI 식권 한 장이다. 오늘 식권과 지난 식권 목록이 같은 모양을 쓴다. {@code shops}는 그때 보여준 순서대로 지금도 추천해도 되는 매장만
 * 담아서, 모두 걸러지면 빈 목록이다. {@code reasons}는 매장 ID를 키로 LLM이 쓴 추천 이유다.
 */
public record MobileRecommendationTicketResult(
        String recommendationId,
        Instant createdAt,
        List<MobileShopSummaryResult> shops,
        List<String> appliedConditions,
        Map<String, String> reasons) {}
