package com.raota.agent.application.recommendation.query;

/** Application-level input for a review summary request. */
public record ReviewSummaryQuery(Long shopId, String focus) {
}
