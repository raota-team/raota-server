package com.raota.mobile.account.application.result;

/** 리프레시 토큰을 교체하며 발급한 새 토큰 쌍이다. */
public record MobileTokenResult(String accessToken, String refreshToken, long expiresIn) {
}
