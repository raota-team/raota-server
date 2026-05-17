package com.raota.presentation.api.auth.dto;

public record AuthTokenResponse(
        String tokenType,
        String accessToken,
        long expiresIn,
        Long memberId
) {
    public static AuthTokenResponse bearer(String accessToken, long expiresIn, Long memberId) {
        return new AuthTokenResponse("Bearer", accessToken, expiresIn, memberId);
    }
}
