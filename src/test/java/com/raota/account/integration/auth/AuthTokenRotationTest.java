package com.raota.account.integration.auth;

import com.raota.account.application.auth.AuthAccountService;
import com.raota.account.application.auth.AuthRefreshSession;
import com.raota.account.domain.member.model.MemberProfile;
import com.raota.account.domain.member.repository.MemberRepository;
import com.raota.account.infrastructure.auth.AuthenticationRequiredException;
import com.raota.account.infrastructure.persistence.auth.RefreshTokenStore;

import com.raota.support.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;


import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;



import org.springframework.transaction.annotation.Transactional;

@Transactional
public class AuthTokenRotationTest extends BaseIntegrationTest {

    @Autowired
    AuthAccountService authAccountService;

    @Autowired
    RefreshTokenStore refreshTokenStore;

    @Autowired
    MemberRepository memberRepository;

    private final String oldToken = "old-token";

    @Test
    void 정상_로테이션_테스트() {
        Long memberId = saveActiveMember();
        refreshTokenStore.save(memberId, oldToken, Instant.now().plusSeconds(3600));

        AuthRefreshSession session = authAccountService.refresh(oldToken);

        assertThat(session.refreshToken()).isNotEqualTo(oldToken);
        assertThat(refreshTokenStore.findByToken(oldToken)).isEmpty();
        assertThat(refreshTokenStore.findByToken(session.refreshToken())).isPresent();
    }

    @Test
    void 만료된_토큰_테스트() {
        Long memberId = saveActiveMember();
        refreshTokenStore.save(memberId, oldToken, Instant.now().minusSeconds(10));

        assertThatThrownBy(() -> authAccountService.refresh(oldToken))
                .isInstanceOf(AuthenticationRequiredException.class)
                .hasMessageContaining("만료된 리프레시 토큰입니다.");

        assertThat(refreshTokenStore.findByToken(oldToken)).isEmpty();
    }

    @Test
    void 유효하지_않은_토큰_테스트() {
        String invalidToken = "none-exists-token";

        assertThatThrownBy(() -> authAccountService.refresh(invalidToken))
                .isInstanceOf(AuthenticationRequiredException.class)
                .hasMessageContaining("유효하지 않은 리프레시 토큰입니다.");
    }

    private Long saveActiveMember() {
        return memberRepository.save(MemberProfile.builder()
                .nickname("토큰테스터")
                .build()).getId();
    }
}
