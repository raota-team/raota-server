package com.raota.presentation.api.community.response;

import com.raota.application.community.result.RamenShopOptionResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record CommunityRamenShopOptionResponse(
        @Schema(description = "라멘집 ID")
        Long id,
        @Schema(description = "가게 이름")
        String name,
        @Schema(description = "지역")
        String region,
        @Schema(description = "썸네일 이미지 URL")
        String thumbnailUrl
) {
    public static CommunityRamenShopOptionResponse from(RamenShopOptionResult result) {
        return new CommunityRamenShopOptionResponse(
                result.id(),
                result.name(),
                result.region(),
                result.thumbnailUrl()
        );
    }
}
