package com.raota.web.account.application.auth;

public record AuthRefreshSession(Long memberId, String refreshToken) {
}
