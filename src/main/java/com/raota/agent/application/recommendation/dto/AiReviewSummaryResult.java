package com.raota.agent.application.recommendation.dto;

public record AiReviewSummaryResult(
        AiReviewSummary summary
) {
    public record AiReviewSummary(
            AiSummaryDetail pros,
            AiSummaryDetail cons,
            AiSummaryDetail recommendedMenu
    ) {
    }

    public record AiSummaryDetail(
            String title,
            String body
    ) {
    }
}
