package com.raota.presentation.api.recommendation.response;

import java.util.List;

public record TasteRecommendationResponse(List<RecommendedShopResponse> recommendedShops) {
    public record RecommendedShopResponse(Long id, String name, String type, String location, String description, String imageUrl, Integer matchScore, boolean isBookmarked) {}
}
