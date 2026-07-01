package com.raota.presentation.admin.user.response;

import com.raota.domain.auth.model.SocialAccount;
import com.raota.domain.member.model.MemberProfile;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record AdminUserListItemResponse(
        Long memberId,
        String nickname,
        String email,
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
                providers.isBlank() ? "-" : providers,
                member.getCreatedAt(),
                member.getUpdatedAt(),
                member.isRegistrationCompleted(),
                member.isDeleted()
        );
    }
}
