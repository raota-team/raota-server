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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 모바일 회원의 프로필과 가입 상태를 보관한다.
 */
@Entity
@Table(name = "tb_v2_user")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class MobileUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Column(name = "nickname_normalized", length = 50)
    private String nicknameNormalized;

    @Column(name = "avatar_url", length = 1000)
    private String avatarUrl;

    @Column(name = "bio", length = 500)
    private String bio;

    @Column(name = "favorite_ramen_type", length = 50)
    private String favoriteRamenType;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MobileUserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MobileUserStatus status;

    @Column(name = "log_count", nullable = false)
    private int logCount;

    @Column(name = "onboarding_completed_at")
    private Instant onboardingCompletedAt;

    @Column(name = "withdrawal_requested_at")
    private Instant withdrawalRequestedAt;

    @Column(name = "purge_scheduled_at")
    private Instant purgeScheduledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private MobileUser(String email) {
        this.email = email;
        this.role = MobileUserRole.USER;
        this.status = MobileUserStatus.ONBOARDING;
        this.logCount = 0;
    }

    public static MobileUser onboarding(String email) {
        return new MobileUser(email);
    }

}
