package com.raota.mobile.account.infrastructure.auth;

import com.raota.global.security.BearerTokenAuthenticator;
import com.raota.global.security.jwt.JwtTokenException;
import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.model.MobileUserStatus;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import com.raota.mobile.common.presentation.MobileApiPath;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** 모바일 API에서는 모바일 발급자의 토큰과 현재 회원 상태를 함께 확인한다. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class MobileBearerTokenAuthenticator implements BearerTokenAuthenticator {

    private static final String UNAVAILABLE_ACCOUNT_MESSAGE = "사용할 수 없는 계정입니다.";

    private final MobileAccessTokenService accessTokens;

    private final MobileUserRepository users;

    @Override
    public boolean supports(HttpServletRequest request) {
        return MobileApiPath.matches(request);
    }

    @Override
    public Authentication authenticate(String token) {
        Long userId;
        try {
            userId = accessTokens.verify(token);
        }
        catch (JwtTokenException exception) {
            throw new MobileAuthenticationException("유효하지 않은 액세스 토큰입니다.", exception);
        }

        MobileUser user = users.findById(userId)
            .orElseThrow(() -> new MobileAuthenticationException(UNAVAILABLE_ACCOUNT_MESSAGE, null));
        if (user.getStatus() != MobileUserStatus.ONBOARDING && user.getStatus() != MobileUserStatus.ACTIVE) {
            throw new MobileAuthenticationException(UNAVAILABLE_ACCOUNT_MESSAGE, null);
        }

        MobileAuthenticatedUser principal = MobileAuthenticatedUser.of(user);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.authorities());
    }

}
