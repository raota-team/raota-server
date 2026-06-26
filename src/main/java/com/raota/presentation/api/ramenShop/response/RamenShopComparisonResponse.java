package com.raota.presentation.api.ramenShop.response;

import java.util.List;

public record RamenShopComparisonResponse(
        ShopComparisonDetail shopA,
        ShopComparisonDetail shopB,
        String focus,
        List<ComparisonNarrative> narratives
) {

    public record ShopComparisonDetail(Long id, String name) {
    }

    public record ComparisonNarrative(String title, String body) {
    }
}
