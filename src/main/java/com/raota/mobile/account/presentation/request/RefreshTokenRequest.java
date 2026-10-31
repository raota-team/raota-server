package com.raota.mobile.account.presentation.request;

import jakarta.validation.constraints.NotBlank;

/** 소비하거나 폐기할 기기의 리프레시 토큰. 비어 있으면 400이다. */
public record RefreshTokenRequest(@NotBlank String refreshToken) {}
