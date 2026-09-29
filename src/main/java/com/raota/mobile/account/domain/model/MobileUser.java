package com.raota.mobile.account.domain.model;

import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
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

    /** 변경 요청에서 누락한 필드는 유지하고 빈 문자열만 지운다. */
    public void updateProfile(String email, String avatarUrl, String bio, String favoriteRamenType) {
        if (email != null) {
            this.email = clearIfEmpty(email);
        }
        if (avatarUrl != null) {
            this.avatarUrl = clearIfEmpty(avatarUrl);
        }
        if (bio != null) {
            this.bio = clearIfEmpty(bio);
        }
        if (favoriteRamenType != null) {
            this.favoriteRamenType = clearIfEmpty(favoriteRamenType);
        }
    }

    /** 가입 중인 회원의 표시 이름과 상태를 한 번만 확정한다. */
    public void completeOnboarding(Nickname nickname, Instant now) {
        if (status != MobileUserStatus.ONBOARDING) {
            throw new MobileException(MobileErrorCode.CONFLICT, "이미 온보딩을 마쳤습니다.");
        }
        this.nickname = nickname.display();
        this.nicknameNormalized = nickname.normalized();
        this.status = MobileUserStatus.ACTIVE;
        this.onboardingCompletedAt = now;
    }

    private String clearIfEmpty(String value) {
        return value.isEmpty() ? null : value;
    }

}
