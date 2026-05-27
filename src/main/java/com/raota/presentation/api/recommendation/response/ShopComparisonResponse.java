package com.raota.presentation.api.recommendation.response;
import java.util.List;
import java.util.Map;

public record ShopComparisonResponse(ShopComparisonDetail shopA, ShopComparisonDetail shopB, List<ComparisonNarrative> narratives) {
    public record ShopComparisonDetail(Long id, String name, Map<String, Integer> scores, Double totalIndex) {}
    public record ComparisonNarrative(String title, String body) {}
}
