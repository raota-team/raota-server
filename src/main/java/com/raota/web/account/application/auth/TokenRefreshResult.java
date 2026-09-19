package com.raota.web.account.application.auth;

public record TokenRefreshResult(
        Long memberId,
        String accessToken,
        long accessTokenExpiresIn,
        String refreshToken
) {
}
