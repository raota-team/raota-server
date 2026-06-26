package com.raota.application.ramenShop.result;

public record AiRamenShopSearchHit(
        Long shopId,
        RamenShopSearchDocument document,
        double finalScore
) {
}
