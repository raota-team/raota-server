package com.raota.domain.auth.service;

public record OAuth2LoginResult(
        Long memberId,
        boolean newMember,
        String accessToken,
        long accessTokenExpiresIn,
        String refreshToken
) {
}
