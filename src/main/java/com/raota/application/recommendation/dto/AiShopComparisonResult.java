package com.raota.application.recommendation.dto;

import java.util.List;
import java.util.Map;

public record AiShopComparisonResult(
        AiShopScores shopA,
        AiShopScores shopB,
        List<AiComparisonNarrative> narratives
) {

    public record AiShopScores(
            Map<String, Integer> scores
    ) {
    }

    public record AiComparisonNarrative(
            String title,
            String body
    ) {
    }
}
