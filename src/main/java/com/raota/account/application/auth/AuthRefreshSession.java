package com.raota.account.application.auth;

public record AuthRefreshSession(Long memberId, String refreshToken) {
}
