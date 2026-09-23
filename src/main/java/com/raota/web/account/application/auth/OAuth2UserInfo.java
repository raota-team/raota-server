package com.raota.web.account.application.auth;

import com.raota.web.account.domain.auth.model.AuthProvider;

public record OAuth2UserInfo(AuthProvider provider, String providerUserId, String email, String nickname,
        String profileImageUrl) {
}
