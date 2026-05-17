package com.raota.presentation.api.recommendation.response;

import java.util.List;

public record ReviewSummaryResponse(AiShopBasicInfo shopInfo, Integer reviewCount, AiSummary summary, List<SampleReview> sampleReviews) {
    public record AiShopBasicInfo(Long id, String name, String type, String location, String imageUrl, boolean isBookmarked) {}
    public record AiSummary(SummaryDetail pros, SummaryDetail cons, SummaryDetail recommendedMenu) {}
    public record SummaryDetail(String title, String body) {}
    public record SampleReview(String name, Integer rating, String text) {}
}
