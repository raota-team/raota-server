package com.raota.presentation.api.discovery.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record TrendingTagResponse(
        @Schema(description = "순위")
        int rank,
        @Schema(description = "태그 이름")
        String name,
        @Schema(description = "트렌드 (up, down, new, same)")
        String trend
) {
}
