package com.raota.account.domain.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "tb_social_account",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_social_account_provider_user", columnNames = {"provider", "provider_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider;

    @Column(name = "provider_id", nullable = false, length = 100)
    private String providerUserId;

    private String email;

    @Column(nullable = false)
    private String nickname;

    private String profileImageUrl;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Builder
    public SocialAccount(
            AuthProvider provider,
            String providerUserId,
            String email,
            String nickname,
            String profileImageUrl,
            Long memberId
    ) {
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.memberId = memberId;
    }

    public void updateProfile(String email, String nickname, String profileImageUrl) {
        if (email != null && !email.isBlank()) {
            this.email = email;
        }
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
    }

    public void updateMemberId(Long memberId) {
        this.memberId = memberId;
    }
}
