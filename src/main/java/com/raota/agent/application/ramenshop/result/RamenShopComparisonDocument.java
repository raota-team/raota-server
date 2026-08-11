package com.raota.agent.application.ramenshop.result;

import java.util.Map;

public record RamenShopComparisonDocument(
        String text,
        Map<String, Object> metadata
) {
}
