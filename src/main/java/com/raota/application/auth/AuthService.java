package com.raota.application.auth;

import com.raota.domain.auth.model.SocialAccount;
import com.raota.application.member.MemberProvisioningService;
import com.raota.domain.member.model.MemberProfile;
import com.raota.infrastructure.auth.WithdrawnMemberException;
import com.raota.infrastructure.auth.JwtTokenProvider;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 관련 비즈니스 로직을 총괄하는 서비스.
 * 소셜 로그인 정보로부터 회원을 식별하고, JWT 토큰의 생명주기를 관리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final MemberProvisioningService memberProvisioningService;
    private final AuthAccountService authAccountService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 소셜 사용자 정보를 바탕으로 로그인을 수행한다.
     * 1. 기존 소셜 계정 확인
     * 2. 회원 데이터(MemberProfile) 조회 또는 신규 생성
     * 3. Access/Refresh 토큰 발급
     */
    public OAuth2LoginResult login(OAuth2UserInfo userInfo) {
        // 소셜 제공자(provider)와 고유 ID로 우리 DB에서 계정을 찾는다.
        Optional<SocialAccount> socialAccountOptional = authAccountService.findSocialAccount(userInfo);
        Optional<MemberProfile> existingMemberProfile = socialAccountOptional
                .flatMap(socialAccount -> memberProvisioningService.findById(socialAccount.getMemberId()));

        existingMemberProfile
                .filter(MemberProfile::isDeleted)
                .ifPresent(member -> {
                    throw new WithdrawnMemberException(com.raota.application.member.MemberLifecycleService.WITHDRAWN_MEMBER_MESSAGE);
                });

        String normalizedNickname = normalizeNickname(userInfo);

        // 이미 가입된 계정이면 기존 회원 정보를, 없으면 신규 회원을 생성한다.
        MemberProfile memberProfile = existingMemberProfile
                .orElseGet(() -> memberProvisioningService.createOAuthMember(normalizedNickname, userInfo.profileImageUrl()));

        // 가입 완료 여부에 따라 신규 회원 여부를 판단한다.
        boolean newMember = !memberProfile.isRegistrationCompleted();

        // 우리 서비스 전용 Access Token(JWT) 생성
        String accessToken = jwtTokenProvider.createAccessToken(memberProfile.getId());

        // 소셜 계정 정보를 업데이트하고, Refresh Token을 생성/저장한다.
        String refreshTokenValue;
        try {
            refreshTokenValue = authAccountService.login(userInfo, normalizedNickname, memberProfile.getId());
        } catch (RuntimeException exception) {
            // 토큰 저장 중 실패 시, 새로 생성한 회원 정보가 남아있지 않도록 롤백 처리를 수행한다.
            if (newMember) {
                memberProvisioningService.deleteById(memberProfile.getId());
            }
            throw exception;
        }

        return new OAuth2LoginResult(
                memberProfile.getId(),
                newMember,
                accessToken,
                jwtTokenProvider.accessTokenExpirySeconds(),
                refreshTokenValue
        );
    }

    /**
     * Refresh Token을 사용하여 Access/Refresh 토큰을 갱신한다. (Token Rotation 적용)
     */
    public TokenRefreshResult refresh(String refreshTokenValue) {
        AuthRefreshSession refreshSession = authAccountService.refresh(refreshTokenValue);

        return new TokenRefreshResult(
                refreshSession.memberId(),
                jwtTokenProvider.createAccessToken(refreshSession.memberId()),
                jwtTokenProvider.accessTokenExpirySeconds(),
                refreshSession.refreshToken()
        );
    }

    /**
     * 로그아웃 처리를 수행하여 서버 측의 Refresh Token을 무효화한다.
     */
    public void logout(String refreshTokenValue) {
        authAccountService.logout(refreshTokenValue);
    }

    private String normalizeNickname(OAuth2UserInfo userInfo) {
        String candidate = userInfo.nickname();
        if (candidate == null || candidate.isBlank()) {
            candidate = userInfo.provider().name().toLowerCase() + "_" + userInfo.providerUserId();
        }
        String normalized = candidate.trim();
        return normalized.length() > 20 ? normalized.substring(0, 20) : normalized;
    }
}
