package com.raota.presentation.admin.ramenShop.request;

import jakarta.validation.constraints.NotNull;

public record RamenShopVisibilityUpdateRequest(
        @NotNull Boolean published
) {
}
