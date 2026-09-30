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

class MobileBearerTokenAuthenticatorTest {

    private final MobileAccessTokenService tokens = mock(MobileAccessTokenService.class);

    private final MobileUserRepository users = mock(MobileUserRepository.class);

    private final MobileBearerTokenAuthenticator authenticator = new MobileBearerTokenAuthenticator(tokens, users);

    @Test
    void 활성_ADMIN은_사용자와_관리자_권한을_함께_가진다() {
        MobileUser user = mock(MobileUser.class);
        when(tokens.verify("valid-token")).thenReturn(42L);
        when(users.findById(42L)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(42L);
        when(user.getStatus()).thenReturn(MobileUserStatus.ACTIVE);
        when(user.getRole()).thenReturn(MobileUserRole.ADMIN);

        var authentication = authenticator.authenticate("valid-token");

        assertThat(authentication.getPrincipal()).isInstanceOf(MobileAuthenticatedUser.class);
        assertThat(authentication.getAuthorities()).extracting(authority -> authority.getAuthority())
            .containsExactly("ROLE_USER", "ROLE_ADMIN");
    }

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

}
