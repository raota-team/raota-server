package com.raota.application.ramenShop.result;

import java.util.List;

public record AiRamenShopComparisonResult(
        List<AiComparisonNarrative> narratives
) {

    public record AiComparisonNarrative(
            String title,
            String body
    ) {
    }
}
