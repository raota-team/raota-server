package com.raota.domain.auth.service;

public record AuthRefreshSession(Long memberId, String refreshToken) {
}
