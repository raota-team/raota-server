package com.raota.agent.application.ramenshop.query;

import java.util.List;

public record ParsedAiRamenShopSearchQuery(
        String normalizedQuery,
        String expandedQuery,
        List<String> foodTypes,
        List<String> foodKeywords,
        List<String> regions
) {
}
