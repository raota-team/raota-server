package com.raota.agent.domain.recommendation.model;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
public class DailyCuration {
    private final Long id;
    private final Integer dateKey;
    private final RamenType ramenType;
    private final String title;
    private final String reason;
    private final String customImageUrl;
    private final LocalDateTime createdAt;

    @Builder
    public DailyCuration(Long id, Integer dateKey, RamenType ramenType, String title, String reason, String customImageUrl, LocalDateTime createdAt) {
        this.id = id;
        this.dateKey = dateKey;
        this.ramenType = ramenType;
        this.title = title;
        this.reason = reason;
        this.customImageUrl = customImageUrl;
        this.createdAt = createdAt;
    }

    public String getEffectiveImageUrl() {
        return (customImageUrl != null && !customImageUrl.isBlank()) ? customImageUrl : ramenType.getImageUrl();
    }
}
