package com.raota.web.account.infrastructure.auth;

import com.raota.web.account.infrastructure.auth.repository.HttpCookieOAuth2AuthorizationRequestRepository;
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

    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    private final OAuth2RedirectTargetResolver redirectTargetResolver;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException, ServletException {
        // 임시 쿠키를 지우기 전에 돌아갈 주소를 고른다.
        String targetUri =
                redirectTargetResolver.resolve(request, authProperties.oauth2().failureRedirectUri());

        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);

        String redirectUri = targetUri + "#error=" + URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8)
                + "&provider=" + URLEncoder.encode(resolveProvider(request), StandardCharsets.UTF_8);
        getRedirectStrategy().sendRedirect(request, response, redirectUri);
    }

    private String resolveProvider(HttpServletRequest request) {
        String uri = request.getRequestURI();
        int lastSlashIndex = uri.lastIndexOf('/');
        if (lastSlashIndex < 0) {
            return "unknown";
        }
        return uri.substring(lastSlashIndex + 1);
    }
}
