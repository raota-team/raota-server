package com.raota.presentation.api.discovery.response;

import com.raota.domain.recommendation.model.WeekendCuration;
import lombok.Builder;
import lombok.Getter;

@Getter
public class WeekendRecommendationResponse {
    private final String id;
    private final String name;
    private final String location;
    private final String imageUrl;
    private final String reason;

    @Builder
    public WeekendRecommendationResponse(String id, String name, String location, String imageUrl, String reason) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.imageUrl = imageUrl;
        this.reason = reason;
    }

    public static WeekendRecommendationResponse from(WeekendCuration curation) {
        return WeekendRecommendationResponse.builder()
                .id(curation.getRamenType().getId().toString())
                .name(curation.getRamenType().getName())
                .location(curation.getRamenType().getSubTitle())
                .imageUrl(curation.getEffectiveImageUrl())
                .reason(curation.getReason())
                .build();
    }
}
