package com.raota.application.recommendation.dto;

import lombok.Builder;

/**
 * @param ramenTypeName 예: 돈코츠 라멘, 쇼유 라멘
 * @param title         AI가 생성한 감성적인 제목
 * @param reason        상세 추천 사유
 */
public record AiRamenRecommendationResponse(String ramenTypeName, String title, String reason) {
    @Builder
    public AiRamenRecommendationResponse {
    }
}
