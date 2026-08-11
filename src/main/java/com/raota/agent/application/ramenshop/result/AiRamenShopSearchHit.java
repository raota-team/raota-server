package com.raota.agent.application.ramenshop.result;

public record AiRamenShopSearchHit(
        Long shopId,
        double finalScore
) {
}
