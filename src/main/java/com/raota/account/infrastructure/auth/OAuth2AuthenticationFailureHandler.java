package com.raota.account.infrastructure.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final AuthProperties authProperties;
    private final com.raota.account.infrastructure.auth.repository.HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository; // 추가

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        // 임시 쿠키 삭제 전 타겟 URI 획득
        String targetUri = java.util.Arrays.stream(request.getCookies() != null ? request.getCookies() : new jakarta.servlet.http.Cookie[0])
                .filter(cookie -> com.raota.account.infrastructure.auth.repository.HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME.equals(cookie.getName()))
                .map(jakarta.servlet.http.Cookie::getValue)
                .findFirst()
                .orElse(authProperties.oauth2().failureRedirectUri());

        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);

        String redirectUri = targetUri
                + "#error=" + URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8)
                + "&provider=" + URLEncoder.encode(resolveProvider(request), StandardCharsets.UTF_8);
        getRedirectStrategy().sendRedirect(request, response, redirectUri);
    }

    private String resolveProvider(HttpServletRequest request) {
        String uri = request.getRequestURI();
        int lastSlashIndex = uri.lastIndexOf('/');
        return lastSlashIndex >= 0 ? uri.substring(lastSlashIndex + 1) : "unknown";
    }
}
