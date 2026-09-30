package com.raota.web.account.infrastructure.auth;

import com.raota.global.security.BearerTokenAuthenticator;
import com.raota.web.account.application.member.MemberLifecycleService;
import com.raota.web.account.application.member.MemberProvisioningService;
import com.raota.web.account.domain.member.model.MemberRole;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class WebBearerTokenAuthenticator implements BearerTokenAuthenticator {

    private final JwtTokenProvider jwtTokenProvider;

    private final MemberProvisioningService memberProvisioningService;

    @Override
    public boolean supports(HttpServletRequest request) {
        return true;
    }

    @Override
    public Authentication authenticate(String token) {
        Long memberId = jwtTokenProvider.getMemberId(token);
        MemberRole role = memberProvisioningService.findActiveMemberRole(memberId)
            .orElseThrow(() -> new JwtAuthenticationException(MemberLifecycleService.WITHDRAWN_MEMBER_MESSAGE,
                    new IllegalStateException("inactive member")));
        AuthenticatedMember member = AuthenticatedMember.of(memberId, role);
        return new UsernamePasswordAuthenticationToken(member, null, member.authorities());
    }

}
