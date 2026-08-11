package com.raota.account.infrastructure.auth;

import com.raota.account.application.auth.AuthService;
import com.raota.account.application.auth.OAuth2LoginResult;
import com.raota.account.application.auth.OAuth2UserInfo;
import com.raota.account.application.auth.OAuth2UserInfoFactory;
import com.raota.account.infrastructure.auth.repository.HttpCookieOAuth2AuthorizationRequestRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;
    private final OAuth2UserInfoFactory oAuth2UserInfoFactory;
    private final RefreshTokenCookieManager refreshTokenCookieManager;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;
    private final AuthProperties authProperties;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2AuthenticationToken oauth2Authentication = (OAuth2AuthenticationToken) authentication;
        OAuth2User principal = oauth2Authentication.getPrincipal();
        String targetUri = getTargetUriFromCookie(request);

        String registrationId = oauth2Authentication.getAuthorizedClientRegistrationId();
        try {
            OAuth2UserInfo userInfo = oAuth2UserInfoFactory.from(
                    registrationId,
                    principal.getAttributes()
            );

            OAuth2LoginResult loginResult = authService.login(userInfo);
            response.addHeader("Set-Cookie", refreshTokenCookieManager.createRefreshTokenCookie(loginResult.refreshToken()).toString());

            httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);

            getRedirectStrategy().sendRedirect(request, response, buildSuccessRedirectUri(targetUri, loginResult, registrationId));
        } catch (AuthenticationRequiredException exception) {
            httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
            getRedirectStrategy().sendRedirect(request, response, buildErrorRedirectUri(targetUri, exception.getMessage(), registrationId));
        }
    }

    private String getTargetUriFromCookie(HttpServletRequest request) {
        String targetUri = Arrays.stream(request.getCookies() != null ? request.getCookies() : new Cookie[0])
                .filter(cookie -> HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(authProperties.oauth2().redirectUri());
        String targetOrigin = getOrigin(targetUri);
        boolean isAllowed = authProperties.cors().allowedOrigins().stream()
                .anyMatch(allowedOrigin->allowedOrigin.equalsIgnoreCase(targetOrigin));

        if(isAllowed) return targetUri;

        return authProperties.oauth2().redirectUri();
    }

    private String buildSuccessRedirectUri(String targetUri, OAuth2LoginResult loginResult, String registrationId) {
        return targetUri
                + "#accessToken=" + encode(loginResult.accessToken())
                + "&tokenType=Bearer"
                + "&expiresIn=" + loginResult.accessTokenExpiresIn()
                + "&memberId=" + loginResult.memberId()
                + "&newMember=" + loginResult.newMember()
                + "&provider=" + encode(registrationId);
    }

    private String buildErrorRedirectUri(String targetUri, String message, String registrationId) {
        return targetUri
                + "#error=" + encode(message)
                + "&provider=" + encode(registrationId);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String getOrigin(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            int port = uri.getPort();

            StringBuilder origin = new StringBuilder();
            origin.append(scheme).append("://").append(host);
            if(port!=-1) origin.append(":").append(port);

            return origin.toString();
        }catch (URISyntaxException e){
            return "";
        }
    }
}
