package com.raota.web.ramenshop.presentation.admin.request;

import jakarta.validation.constraints.NotNull;

public record RamenShopVisibilityUpdateRequest(@NotNull Boolean published) {
}
