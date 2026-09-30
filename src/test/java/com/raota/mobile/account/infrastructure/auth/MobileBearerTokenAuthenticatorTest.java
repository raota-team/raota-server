package com.raota.mobile.account.infrastructure.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.model.MobileUserRole;
import com.raota.mobile.account.domain.model.MobileUserStatus;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;

class MobileBearerTokenAuthenticatorTest {

    private final MobileAccessTokenService tokens = mock(MobileAccessTokenService.class);

    private final MobileUserRepository users = mock(MobileUserRepository.class);

    private final MobileBearerTokenAuthenticator authenticator = new MobileBearerTokenAuthenticator(tokens, users);

    @Test
    void 모바일_API_경로만_담당한다() {
        MockHttpServletRequest root = new MockHttpServletRequest("GET", "/app/api/v2");
        root.setContextPath("/app");
        MockHttpServletRequest child = new MockHttpServletRequest("GET", "/api/v2/account");
        MockHttpServletRequest other = new MockHttpServletRequest("GET", "/api/v2extra");

        assertThat(authenticator.supports(root)).isTrue();
        assertThat(authenticator.supports(child)).isTrue();
        assertThat(authenticator.supports(other)).isFalse();
    }

    @Test
    void 활성_ADMIN은_사용자_관리자_활성_회원_권한을_함께_가진다() {
        assertThat(authenticate(MobileUserStatus.ACTIVE, MobileUserRole.ADMIN).getAuthorities())
            .extracting(authority -> authority.getAuthority())
            .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN", "ACTIVE_MEMBER");
    }

    @Test
    void 가입_중인_회원은_활성_회원_권한_대신_가입_중_권한을_가진다() {
        assertThat(authenticate(MobileUserStatus.ONBOARDING, MobileUserRole.USER).getAuthorities())
            .extracting(authority -> authority.getAuthority())
            .containsExactlyInAnyOrder("ROLE_USER", "ONBOARDING");
    }

    private Authentication authenticate(MobileUserStatus status, MobileUserRole role) {
        MobileUser user = mock(MobileUser.class);
        when(tokens.verify("valid-token")).thenReturn(42L);
        when(users.findById(42L)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(42L);
        when(user.getStatus()).thenReturn(status);
        when(user.getRole()).thenReturn(role);
        return authenticator.authenticate("valid-token");
    }

}
