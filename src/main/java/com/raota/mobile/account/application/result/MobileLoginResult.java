package com.raota.mobile.account.application.result;

/** 로그인 후 발급한 토큰과 현재 회원 정보다. */
public record MobileLoginResult(String accessToken, String refreshToken, long expiresIn, boolean isNewMember,
        MobileMemberResult member) {
}
