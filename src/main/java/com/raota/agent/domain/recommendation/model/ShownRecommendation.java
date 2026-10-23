package com.raota.agent.domain.recommendation.model;

import java.time.Instant;
import java.util.List;

/**
 * 매장을 하나 이상 보여준 추천 기록이다. 앱의 식권 한 장에 해당한다. {@code located}는 좌표로 가까운 순 추천을 받았는지다.
 */
public record ShownRecommendation(Long id, String conditionsJson, boolean located, Instant createdAt,
        List<MobileRecommendationRun.Item> items) {
}
