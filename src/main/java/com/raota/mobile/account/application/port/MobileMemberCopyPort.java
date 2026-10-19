package com.raota.mobile.account.application.port;

import com.raota.mobile.account.domain.model.MobileUserStatus;
import com.raota.mobile.account.domain.model.Nickname;
import com.raota.mobile.account.domain.model.OAuthProvider;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** v1 회원·소셜 계정 테이블을 읽고 ID를 유지해 v2 회원 테이블에 쓴다. 시각은 저장된 값 그대로 주고받는다. */
public interface MobileMemberCopyPort {

    /** 탈퇴 회원을 포함한 v1 회원 전체를 ID 순으로 읽는다. */
    List<LegacyMember> legacyMembers();

    /** v1 소셜 계정 전체를 ID 순으로 읽는다. */
    List<LegacySocialAccount> legacySocialAccounts();

    /** v2 회원 ID별 생성 시각(UTC). */
    Map<Long, LocalDateTime> userCreatedAts();

    List<String> takenNicknames();

    List<OAuthAccountKey> oauthAccounts();

    void insertUser(NewUser user);

    void insertOAuthAccount(NewOAuthAccount account);

    record LegacyMember(Long id, String nickname, String email, String imageUrl, String bio,
            boolean registrationCompleted, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
    }

    record LegacySocialAccount(Long id, Long memberId, String provider, String providerId, String email) {
    }

    record OAuthAccountKey(Long userId, String provider, String subject) {
    }

    /** {@code nickname}이 null이면 닉네임 없이 온보딩 상태로 만든다. 시각은 UTC다. */
    record NewUser(Long id, String email, Nickname nickname, String avatarUrl, String bio, MobileUserStatus status,
            LocalDateTime onboardingCompletedAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
    }

    record NewOAuthAccount(Long userId, OAuthProvider provider, String subject, String email, LocalDateTime linkedAt) {
    }

}
