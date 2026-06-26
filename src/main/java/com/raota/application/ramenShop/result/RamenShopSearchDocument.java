package com.raota.application.ramenShop.result;

import java.util.Map;

public record RamenShopSearchDocument(
        String text,
        Map<String, Object> metadata,
        double score
) {
}
