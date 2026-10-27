package com.raota.mobile.account.application.result;

import com.raota.mobile.account.domain.model.MobileUser;
import java.time.Instant;

/**
 * 로그인과 회원 조회에 쓰는 모바일 회원 프로필이다. 앱은 {@code onboardingCompletedAt}으로 온보딩 완료를,
 * {@code consentRequired}로 현재 약관·처리방침 동의가 필요한지를 판단한다.
 */
public record MobileMemberResult(
        String id,
        String email,
        String nickname,
        String avatarUrl,
        String bio,
        String favoriteRamenType,
        String status,
        int logCount,
        Instant onboardingCompletedAt,
        boolean consentRequired) {

    public static MobileMemberResult from(MobileUser user, boolean consentRequired) {
        return new MobileMemberResult(
                String.valueOf(user.getId()),
                user.getEmail(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getFavoriteRamenType(),
                user.getStatus().name(),
                user.getLogCount(),
                user.getOnboardingCompletedAt(),
                consentRequired);
    }
}
