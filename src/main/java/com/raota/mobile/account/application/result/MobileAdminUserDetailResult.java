package com.raota.mobile.account.application.result;

import com.raota.mobile.account.domain.model.MobileUserRole;
import com.raota.mobile.account.domain.model.MobileUserStatus;
import com.raota.mobile.account.domain.model.OAuthProvider;
import java.time.Instant;
import java.util.List;

/**
 * 관리자 앱 회원 상세다. 목록 항목에 프로필과 연결된 로그인 수단별 시각을 더한다. 제공자 식별자와 Apple 갱신 토큰은 운영 화면에 필요 없어 내보내지 않는다.
 */
public record MobileAdminUserDetailResult(
        String id,
        String nickname,
        String email,
        MobileUserRole role,
        MobileUserStatus status,
        List<OAuthProvider> providers,
        int logCount,
        Instant createdAt,
        Instant onboardingCompletedAt,
        Instant withdrawalRequestedAt,
        Instant purgeScheduledAt,
        Instant lastLoginAt,
        String avatarUrl,
        String bio,
        String favoriteRamenType,
        Instant updatedAt,
        List<OAuthAccount> oauthAccounts) {

    /** {@code email}은 제공자가 알려 준 이메일이다. */
    public record OAuthAccount(OAuthProvider provider, String email, Instant linkedAt, Instant lastLoginAt) {}
}
