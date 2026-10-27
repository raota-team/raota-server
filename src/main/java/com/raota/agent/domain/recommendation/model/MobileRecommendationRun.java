package com.raota.agent.domain.recommendation.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * 추천 한 번에 무엇을 보여줬는지 남기는 노출 기록이다. 순위 학습·A/B에 쓸 수 있도록 입력 조건, 후보 수, 버전, 지연시간과 보여준 매장 순위를
 * 남긴다. 위치는 소수점 2자리(약 1km)로 낮춰 저장한다. 앱은 같은 기록을 "오늘 뽑은 식권"으로 다시 보여준다.
 */
public record MobileRecommendationRun(
        Long userId,
        String conditionsJson,
        BigDecimal latitude,
        BigDecimal longitude,
        int candidateCount,
        String algorithmVersion,
        String indexVersion,
        String modelVersion,
        int latencyMs,
        Instant createdAt,
        List<Item> items) {

    /** 보여준 순서(1부터)와 매장, 순위 점수, LLM이 쓴 추천 이유(없으면 {@code null})다. */
    public record Item(int rank, Long shopId, double score, String reason) {}
}
