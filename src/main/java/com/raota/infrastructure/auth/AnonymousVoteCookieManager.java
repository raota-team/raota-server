package com.raota.infrastructure.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

@Component
public class AnonymousVoteCookieManager {

    public static final String COOKIE_NAME = "raota_anonymous_vote_id";
    private static final long MAX_AGE_SECONDS = 60L * 60 * 24 * 365;

    private final AuthProperties authProperties;

    public AnonymousVoteCookieManager(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public String extractAnonymousVoteId(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, COOKIE_NAME);
        return cookie == null ? null : cookie.getValue();
    }

    public String createAnonymousVoteId() {
        return UUID.randomUUID().toString();
    }

    public ResponseCookie createAnonymousVoteCookie(String anonymousVoteId) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(COOKIE_NAME, anonymousVoteId)
                .httpOnly(true)
                .secure(authProperties.cookie().secure())
                .path("/")
                .sameSite(authProperties.cookie().sameSite())
                .maxAge(MAX_AGE_SECONDS);

        if (authProperties.cookie().domain() != null && !authProperties.cookie().domain().isBlank()) {
            builder.domain(authProperties.cookie().domain());
        }

        return builder.build();
    }
}
