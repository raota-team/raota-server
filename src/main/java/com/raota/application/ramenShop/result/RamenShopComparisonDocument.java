package com.raota.application.ramenShop.result;

import java.util.Map;

public record RamenShopComparisonDocument(
        String text,
        Map<String, Object> metadata
) {
}
