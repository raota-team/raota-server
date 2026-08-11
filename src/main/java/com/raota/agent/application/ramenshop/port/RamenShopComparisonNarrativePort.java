package com.raota.agent.application.ramenshop.port;

import com.raota.agent.application.ramenshop.result.AiRamenShopComparisonResult;

public interface RamenShopComparisonNarrativePort {
    AiRamenShopComparisonResult generateComparisonNarratives(
            String focus,
            String contextA,
            String contextB
    );
}
