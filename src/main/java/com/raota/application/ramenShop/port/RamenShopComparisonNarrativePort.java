package com.raota.application.ramenShop.port;

import com.raota.application.ramenShop.result.AiRamenShopComparisonResult;

public interface RamenShopComparisonNarrativePort {
    AiRamenShopComparisonResult generateComparisonNarratives(
            String focus,
            String contextA,
            String contextB
    );
}
