package com.raota.ramenshop.presentation.admin.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RamenShopVisibilityBulkUpdateRequest(
        @NotNull @Min(1) Long fromId,
        @NotNull @Min(1) Long toId,
        @NotNull Boolean published
) {
}
