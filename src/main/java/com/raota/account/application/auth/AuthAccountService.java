package com.raota.account.application.auth;

import com.raota.account.domain.auth.model.SocialAccount;
import com.raota.account.domain.auth.repository.SocialAccountRepository;
import com.raota.account.domain.member.repository.MemberRepository;
import com.raota.account.infrastructure.persistence.auth.RefreshTokenStore;
import com.raota.account.infrastructure.persistence.auth.StoredRefreshToken;
import com.raota.account.infrastructure.auth.AuthProperties;
import com.raota.account.infrastructure.auth.AuthenticationRequiredException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthAccountService {

    private final SocialAccountRepository socialAccountRepository;
    private final RefreshTokenStore refreshTokenStore;
    private final AuthProperties authProperties;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public Optional<SocialAccount> findSocialAccount(OAuth2UserInfo userInfo) {
        return socialAccountRepository.findByProviderAndProviderUserId(userInfo.provider(), userInfo.providerUserId());
    }

    @Transactional
    public String login(OAuth2UserInfo userInfo, String normalizedNickname, Long memberId) {
        socialAccountRepository.findByProviderAndProviderUserId(userInfo.provider(), userInfo.providerUserId())
                .ifPresentOrElse(
                        socialAccount -> {
                            socialAccount.updateProfile(
                                    userInfo.email(),
                                    normalizedNickname,
                                    userInfo.profileImageUrl()
                            );
                            socialAccount.updateMemberId(memberId);
                        },
                        () -> socialAccountRepository.save(SocialAccount.builder()
                                .provider(userInfo.provider())
                                .providerUserId(userInfo.providerUserId())
                                .email(userInfo.email())
                                .nickname(normalizedNickname)
                                .profileImageUrl(userInfo.profileImageUrl())
                                .memberId(memberId)
                                .build())
                );

        String refreshTokenValue = generateRefreshToken();
        Instant refreshTokenExpiry = Instant.now().plusSeconds(authProperties.refreshTokenExpirySeconds());
        upsertRefreshToken(memberId, refreshTokenValue, refreshTokenExpiry);
        return refreshTokenValue;
    }

    @Transactional
    public AuthRefreshSession refresh(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new AuthenticationRequiredException("리프레시 토큰이 없습니다.");
        }

        StoredRefreshToken refreshToken = refreshTokenStore.findByToken(refreshTokenValue)
                .orElseThrow(() -> new AuthenticationRequiredException("유효하지 않은 리프레시 토큰입니다."));

        if (!memberRepository.existsByIdAndDeletedAtIsNull(refreshToken.memberId())) {
            refreshTokenStore.deleteByToken(refreshTokenValue);
            throw new AuthenticationRequiredException(com.raota.account.application.member.MemberLifecycleService.WITHDRAWN_MEMBER_MESSAGE);
        }

        if (refreshToken.isExpired(Instant.now())) {
            refreshTokenStore.deleteByToken(refreshTokenValue);
            throw new AuthenticationRequiredException("만료된 리프레시 토큰입니다.");
        }

        // [복구] Token Rotation: 보안을 위해 리프레시 시마다 토큰을 새로 교체한다.
        String rotatedRefreshToken = generateRefreshToken();
        refreshTokenStore.save(
                refreshToken.memberId(),
                rotatedRefreshToken,
                Instant.now().plusSeconds(authProperties.refreshTokenExpirySeconds())
        );
        return new AuthRefreshSession(refreshToken.memberId(), rotatedRefreshToken);
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            return;
        }
        refreshTokenStore.deleteByToken(refreshTokenValue);
    }

    @Transactional
    public void logoutByMemberId(Long memberId) {
        refreshTokenStore.findByMemberId(memberId)
                .ifPresent(refreshToken -> refreshTokenStore.deleteByToken(refreshToken.token()));
    }

    private void upsertRefreshToken(Long memberId, String token, Instant expiresAt) {
        refreshTokenStore.save(memberId, token, expiresAt);
    }

    private String generateRefreshToken() {
        return UUID.randomUUID() + UUID.randomUUID().toString().replace("-", "");
    }
}
