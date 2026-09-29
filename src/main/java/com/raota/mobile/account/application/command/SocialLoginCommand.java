package com.raota.mobile.account.application.command;

import com.raota.mobile.account.domain.model.OAuthProvider;

/** 선택한 소셜 제공자와 제시한 인증 정보다. */
public record SocialLoginCommand(OAuthProvider provider, SocialCredential credential) {
}
