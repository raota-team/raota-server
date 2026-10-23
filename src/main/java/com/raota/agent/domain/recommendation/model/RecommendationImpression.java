package com.raota.agent.domain.recommendation.model;

import java.time.Instant;

/**
 * 추천 한 번에 보여준 매장 한 칸이다. 성과 집계에 쓴다. {@code openedAt}은 회원이 추천 결과에서 처음 연 시각(없으면
 * {@code null})이다.
 */
public record RecommendationImpression(Long runId, Long userId, Instant shownAt, String algorithmVersion,
        String conditionsJson, int rank, Long shopId, Instant openedAt) {
}
