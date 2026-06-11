package com.raota.application.recommendation.dto;

import lombok.Builder;

/**
 * @param ramenTypeId 예: iekei, shoyu
 * @param title       AI가 생성한 감성적인 제목
 * @param reason      상세 추천 사유
 */
public record AiRamenRecommendationResponse(String ramenTypeId, String title, String reason) {
    @Builder
    public AiRamenRecommendationResponse {
    }
}
