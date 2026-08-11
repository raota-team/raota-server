package com.raota.agent.presentation.ramenshop.response;

import java.util.List;

public record AiRamenShopSearchResponse(List<RecommendedShopResponse> recommendedShops) {
    public record RecommendedShopResponse(Long id, String name, String type, String location, String description, String imageUrl, Integer matchScore, boolean isBookmarked) {}
}
