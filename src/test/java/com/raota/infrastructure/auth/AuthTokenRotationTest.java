package com.raota.infrastructure.auth;

import com.raota.application.auth.AuthAccountService;
import com.raota.application.auth.AuthRefreshSession;
import com.raota.infrastructure.persistence.auth.RefreshTokenStore;

import com.raota.helper.BaseIntegrationTest;
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

    private final String oldToken = "old-token";
    private final Long memberId = 1L;

    @Test
    void 정상_로테이션_테스트() {
        refreshTokenStore.save(memberId, oldToken, Instant.now().plusSeconds(3600));

        AuthRefreshSession session = authAccountService.refresh(oldToken);

        assertThat(session.refreshToken()).isNotEqualTo(oldToken);
        assertThat(refreshTokenStore.findByToken(oldToken)).isEmpty();
        assertThat(refreshTokenStore.findByToken(session.refreshToken())).isPresent();
    }

    @Test
    void 만료된_토큰_테스트() {
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
}