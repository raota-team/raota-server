package com.raota.presentation.api.community.response;

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
}
