package com.raota.domain.auth.service;

import com.raota.domain.auth.model.AuthProvider;

public record OAuth2UserInfo(
        AuthProvider provider,
        String providerUserId,
        String email,
        String nickname,
        String profileImageUrl
) {
}
