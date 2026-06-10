package com.raota.presentation.api.ramenShop.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record RecentVerifiedShopResponse(
        @Schema(description = "라멘집 ID")
        Long id,
        @Schema(description = "라멘집 이름")
        String name,
        @Schema(description = "라멘집 위치 (시 구)")
        String location,
        @Schema(description = "최근 인증샷 URL")
        String imageUrl,
        @Schema(description = "해당 매장의 총 인증샷 개수")
        long photoCount
) {
}
