package com.raota.application.auth;

public record TokenRefreshResult(
        Long memberId,
        String accessToken,
        long accessTokenExpiresIn,
        String refreshToken
) {
}
