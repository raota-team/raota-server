package com.raota.mobile.account.application.port;

import com.raota.mobile.account.application.command.SocialCredential;
import com.raota.mobile.account.application.result.SocialIdentityResult;
import com.raota.mobile.account.domain.model.OAuthProvider;

/** 소셜 제공자가 발급한 인증 정보를 검증한다. */
public interface SocialTokenVerifier {

    OAuthProvider provider();

    SocialIdentityResult verify(SocialCredential credential);
}
