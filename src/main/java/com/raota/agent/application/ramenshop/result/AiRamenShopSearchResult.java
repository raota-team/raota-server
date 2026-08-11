package com.raota.agent.application.ramenshop.result;

import java.util.List;

public record AiRamenShopSearchResult(List<ShopResult> shops) {
    public record ShopResult(
            Long id,
            String name,
            String type,
            String location,
            String description,
            String imageUrl,
            Integer matchScore,
            boolean bookmarked
    ) {
    }
}
