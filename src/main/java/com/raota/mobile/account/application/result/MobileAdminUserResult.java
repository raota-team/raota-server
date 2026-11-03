package com.raota.mobile.account.application.result;

import com.raota.mobile.account.domain.model.MobileUserRole;
import com.raota.mobile.account.domain.model.MobileUserStatus;
import com.raota.mobile.account.domain.model.OAuthProvider;
import java.time.Instant;
import java.util.List;

/**
 * 관리자 앱 회원 목록의 한 건이다. 연결된 로그인 수단과 마지막 로그인 시각은 OAuth 계정에서 모은다. 탈퇴 처리된 회원은 닉네임·이메일이 null이다.
 */
public record MobileAdminUserResult(
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
        Instant lastLoginAt) {}
