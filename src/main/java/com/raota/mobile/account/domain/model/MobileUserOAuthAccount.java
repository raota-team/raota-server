package com.raota.mobile.account.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 한 회원에게 연결된 외부 인증 제공자의 식별자와 로그인 시각을 보관한다.
 */
@Entity
@Table(name = "tb_v2_user_oauth_account")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class MobileUserOAuthAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private OAuthProvider provider;

    @Column(name = "provider_subject", nullable = false, length = 255)
    private String providerSubject;

    @Column(name = "provider_email", length = 255)
    private String providerEmail;

    @Column(name = "apple_refresh_token_encrypted", length = 2048)
    private String appleRefreshTokenEncrypted;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;

    @Column(name = "last_login_at", nullable = false)
    private Instant lastLoginAt;

    private MobileUserOAuthAccount(Long userId, OAuthProvider provider, String providerSubject, String providerEmail,
            Instant now) {
        this.userId = userId;
        this.provider = provider;
        this.providerSubject = providerSubject;
        this.providerEmail = providerEmail;
        this.linkedAt = now;
        this.lastLoginAt = now;
    }

    public static MobileUserOAuthAccount link(Long userId, OAuthProvider provider, String providerSubject,
            String providerEmail, Instant now) {
        return new MobileUserOAuthAccount(userId, provider, providerSubject, providerEmail, now);
    }

}
