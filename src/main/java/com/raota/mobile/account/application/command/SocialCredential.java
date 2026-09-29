package com.raota.mobile.account.application.command;

/** 소셜 제공자에 제시할 인증 정보다. */
public record SocialCredential(String idToken, String accessToken, String authorizationCode, String nonce) {
}
