package com.raota.mobile.account.application.result;

import com.raota.mobile.account.domain.model.OAuthProvider;

/** 제공자가 검증한 회원 식별자와 이메일이다. */
public record SocialIdentity(OAuthProvider provider, String subject, String email) {
}
