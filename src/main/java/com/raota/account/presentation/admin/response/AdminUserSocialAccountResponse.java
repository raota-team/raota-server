package com.raota.account.presentation.admin.response;

import com.raota.account.domain.auth.model.SocialAccount;

public record AdminUserSocialAccountResponse(
        String provider,
        String providerUserId,
        String email,
        String nickname,
        String profileImageUrl
) {
    public static AdminUserSocialAccountResponse from(SocialAccount socialAccount) {
        return new AdminUserSocialAccountResponse(
                socialAccount.getProvider().name(),
                socialAccount.getProviderUserId(),
                socialAccount.getEmail(),
                socialAccount.getNickname(),
                socialAccount.getProfileImageUrl()
        );
    }
}
