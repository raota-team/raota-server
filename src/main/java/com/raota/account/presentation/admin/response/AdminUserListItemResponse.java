package com.raota.account.presentation.admin.response;

import com.raota.account.domain.auth.model.SocialAccount;
import com.raota.account.domain.member.model.MemberProfile;
import com.raota.account.domain.member.model.MemberRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record AdminUserListItemResponse(
        Long id,
        String nickname,
        String email,
        MemberRole role,
        String providers,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean registrationCompleted,
        boolean deleted
) {
    public static AdminUserListItemResponse from(MemberProfile member, List<SocialAccount> socialAccounts) {
        String providers = socialAccounts.stream()
                .map(socialAccount -> socialAccount.getProvider().name())
                .distinct()
                .collect(Collectors.joining(", "));
        return new AdminUserListItemResponse(
                member.getId(),
                member.getNickname(),
                member.getEmail(),
                member.getRole(),
                providers.isBlank() ? "-" : providers,
                member.getCreatedAt(),
                member.getUpdatedAt(),
                member.isRegistrationCompleted(),
                member.isDeleted()
        );
    }
}
