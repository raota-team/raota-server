package com.raota.agent.application.ramenshop.result;

import java.util.Map;

public record RamenShopSearchDocument(
        String text,
        Map<String, Object> metadata,
        double score
) {
}
