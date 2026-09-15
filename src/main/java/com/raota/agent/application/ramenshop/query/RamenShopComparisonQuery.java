package com.raota.agent.application.ramenshop.query;

/** Application-level input for comparing two ramen shops. */
public record RamenShopComparisonQuery(Long shopAId, Long shopBId, String focus) {
}
