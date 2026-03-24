package com.raota.domain.auth.service;

public record TokenRefreshResult(
        Long memberId,
        String accessToken,
        long accessTokenExpiresIn,
        String refreshToken
) {
}
