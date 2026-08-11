package com.raota.account.infrastructure.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

/**
 * Refresh Token을 브라우저 쿠키에 안전하게 보관하고 관리하는 매니저.
 * HttpOnly, Secure 설정 등을 통해 토큰 탈취를 방지한다.
 */
@Slf4j
@Component
public class RefreshTokenCookieManager {

    private final AuthProperties authProperties;

    public RefreshTokenCookieManager(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    /**
     * 새로운 Refresh Token을 담은 보안 쿠키를 생성한다.
     * - HttpOnly: 자바스크립트에서 접근 불가 (XSS 방지)
     * - Secure: HTTPS 연결에서만 전송
     * - SameSite: CSRF 공격 방지 설정
     */
    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        String sameSite = authProperties.cookie().sameSite();
        boolean secure = authProperties.cookie().secure();
        String domain = authProperties.cookie().domain();

        log.info("Creating refresh token cookie: name={}, domain={}, secure={}, sameSite={}", 
                authProperties.cookie().refreshTokenName(), domain, secure, sameSite);

        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(
                        authProperties.cookie().refreshTokenName(),
                        refreshToken
                )
                .httpOnly(true)
                .secure(secure)
                .path("/") // 모든 경로에서 쿠키 전송 가능
                .sameSite(sameSite)
                .maxAge(authProperties.refreshTokenExpirySeconds());

        // 특정 도메인이 설정되어 있다면 추가한다.
        if (domain != null && !domain.isBlank()) {
            builder.domain(domain);
        }
        return builder.build();
    }

    /**
     * 로그아웃 등을 위해 Refresh Token 쿠키를 삭제하기 위한 만료된 쿠키를 생성한다.
     */
    public ResponseCookie clearRefreshTokenCookie() {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(
                        authProperties.cookie().refreshTokenName(),
                        ""
                )
                .httpOnly(true)
                .secure(authProperties.cookie().secure())
                .path("/")
                .sameSite(authProperties.cookie().sameSite())
                .maxAge(0); // 즉시 만료되도록 설정하여 삭제 유도

        if (authProperties.cookie().domain() != null && !authProperties.cookie().domain().isBlank()) {
            builder.domain(authProperties.cookie().domain());
        }
        return builder.build();
    }

    /**
     * 요청(Request)의 쿠키 목록에서 Refresh Token 값을 추출한다.
     */
    public String extractRefreshToken(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, authProperties.cookie().refreshTokenName());
        return cookie == null ? null : cookie.getValue();
    }
}
