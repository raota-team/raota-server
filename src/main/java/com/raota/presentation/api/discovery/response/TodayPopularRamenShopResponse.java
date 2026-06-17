package com.raota.presentation.api.discovery.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record TodayPopularRamenShopResponse(
        @Schema(description = "라멘집 ID")
        Long ramenShopId,
        @Schema(description = "라멘집 이름")
        String name
) {
}
