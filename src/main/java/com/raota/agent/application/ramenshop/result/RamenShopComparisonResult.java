package com.raota.agent.application.ramenshop.result;

import java.util.List;

public record RamenShopComparisonResult(
        ShopSummary shopA,
        ShopSummary shopB,
        String focus,
        List<ComparisonNarrative> narratives
) {

    public record ShopSummary(Long id, String name) {
    }

    public record ComparisonNarrative(String title, String body) {
    }
}
