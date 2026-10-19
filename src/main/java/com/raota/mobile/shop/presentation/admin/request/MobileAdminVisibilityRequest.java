package com.raota.mobile.shop.presentation.admin.request;

import jakarta.validation.constraints.NotNull;

public record MobileAdminVisibilityRequest(@NotNull Boolean published) {
}
