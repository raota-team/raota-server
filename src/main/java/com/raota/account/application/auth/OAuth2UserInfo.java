package com.raota.account.application.auth;

import com.raota.account.domain.auth.model.AuthProvider;

public record OAuth2UserInfo(
        AuthProvider provider,
        String providerUserId,
        String email,
        String nickname,
        String profileImageUrl
) {
}
