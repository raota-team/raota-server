package com.raota.mobile.account.presentation.request;

import com.raota.mobile.account.application.command.SocialCredential;
import com.raota.mobile.account.application.command.SocialLoginCommand;
import com.raota.mobile.account.domain.model.OAuthProvider;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 제공자별 인증 정보를 제출한다. 사용하지 않는 토큰은 비워 둘 수 있다. */
public record SocialLoginRequest(@NotNull OAuthProvider provider, @Size(max = 8192) String idToken,
        @Size(max = 8192) String accessToken, @Size(max = 8192) String authorizationCode,
        @Size(max = 8192) String nonce) {

    public SocialLoginCommand toCommand() {
        return new SocialLoginCommand(provider, new SocialCredential(idToken, accessToken, authorizationCode, nonce));
    }

}
