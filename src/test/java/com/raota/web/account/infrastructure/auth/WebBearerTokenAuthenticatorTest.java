package com.raota.web.account.infrastructure.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.raota.web.account.application.member.MemberLifecycleService;
import com.raota.web.account.application.member.MemberProvisioningService;
import com.raota.web.account.domain.member.model.MemberRole;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class WebBearerTokenAuthenticatorTest {

    private final JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);

    private final MemberProvisioningService members = mock(MemberProvisioningService.class);

    private final WebBearerTokenAuthenticator authenticator = new WebBearerTokenAuthenticator(tokenProvider, members);

    @Test
    void 활성_ADMIN의_현재_DB_역할을_권한으로_설정한다() {
        when(tokenProvider.getMemberId("valid-token")).thenReturn(1L);
        when(members.findActiveMemberRole(1L)).thenReturn(Optional.of(MemberRole.ADMIN));

        var authentication = authenticator.authenticate("valid-token");

        assertThat(authentication.getPrincipal()).isInstanceOf(AuthenticatedMember.class);
        assertThat(authentication.getAuthorities()).extracting(authority -> authority.getAuthority())
            .containsExactly("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void 탈퇴한_회원의_토큰을_거절한다() {
        when(tokenProvider.getMemberId("withdrawn-token")).thenReturn(1L);
        when(members.findActiveMemberRole(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticator.authenticate("withdrawn-token"))
            .isInstanceOf(JwtAuthenticationException.class)
            .hasMessage(MemberLifecycleService.WITHDRAWN_MEMBER_MESSAGE);
    }

}
