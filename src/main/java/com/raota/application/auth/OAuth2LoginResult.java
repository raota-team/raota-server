package com.raota.application.auth;

public record OAuth2LoginResult(
        Long memberId,
        boolean newMember,
        String accessToken,
        long accessTokenExpiresIn,
        String refreshToken
) {
}
