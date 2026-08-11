package com.raota.agent.domain.recommendation.model;

import lombok.Builder;
import lombok.Getter;

@Getter
public class RamenType {
    private final Long id;
    private final String name;
    private final String subTitle;
    private final String imageUrl;

    @Builder
    public RamenType(Long id, String name, String subTitle, String imageUrl) {
        this.id = id;
        this.name = name;
        this.subTitle = subTitle;
        this.imageUrl = imageUrl;
    }
}
