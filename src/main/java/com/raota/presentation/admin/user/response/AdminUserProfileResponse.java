package com.raota.presentation.admin.user.response;

import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.model.MemberRole;
import java.time.LocalDateTime;

public record AdminUserProfileResponse(
        Long memberId,
        String nickname,
        String email,
        MemberRole role,
        String imageUrl,
        String backgroundImageUrl,
        String bio,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt,
        boolean registrationCompleted,
        boolean deleted
) {
    public static AdminUserProfileResponse from(MemberProfile member, String imageUrl, String backgroundImageUrl) {
        return new AdminUserProfileResponse(
                member.getId(),
                member.getNickname(),
                member.getEmail(),
                member.getRole(),
                imageUrl,
                backgroundImageUrl,
                member.getBio(),
                member.getCreatedAt(),
                member.getUpdatedAt(),
                member.getDeletedAt(),
                member.isRegistrationCompleted(),
                member.isDeleted()
        );
    }
}
