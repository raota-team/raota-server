package com.raota.application.auth;

public record AuthRefreshSession(Long memberId, String refreshToken) {
}
