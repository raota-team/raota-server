package com.raota.discovery.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record DiscoveryStatsResponse(
        @Schema(description = "등록된 라멘집 수")
        long totalShops,
        @Schema(description = "누적 리뷰(또는 AI 분석) 수")
        long totalReviews,
        @Schema(description = "활동 중인 유저 수")
        long totalUsers
) {
}
