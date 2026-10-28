package com.raota.web.account.infrastructure.auth;

import com.raota.web.account.application.auth.AuthService;
import com.raota.web.account.application.auth.OAuth2LoginResult;
import com.raota.web.account.application.auth.OAuth2UserInfo;
import com.raota.web.account.application.auth.OAuth2UserInfoFactory;
import com.raota.web.account.infrastructure.auth.repository.HttpCookieOAuth2AuthorizationRequestRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    private final OAuth2RedirectTargetResolver redirectTargetResolver;

    private final AuthProperties authProperties;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        OAuth2AuthenticationToken oauth2Authentication = (OAuth2AuthenticationToken) authentication;
        OAuth2User principal = oauth2Authentication.getPrincipal();
        String targetUri =
                redirectTargetResolver.resolve(request, authProperties.oauth2().redirectUri());

        String registrationId = oauth2Authentication.getAuthorizedClientRegistrationId();
        try {
            OAuth2UserInfo userInfo = oAuth2UserInfoFactory.from(registrationId, principal.getAttributes());

            OAuth2LoginResult loginResult = authService.login(userInfo);
            response.addHeader(
                    "Set-Cookie",
                    refreshTokenCookieManager
                            .createRefreshTokenCookie(loginResult.refreshToken())
                            .toString());

            httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);

            getRedirectStrategy()
                    .sendRedirect(request, response, buildSuccessRedirectUri(targetUri, loginResult, registrationId));
        } catch (AuthenticationRequiredException exception) {
            httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
            getRedirectStrategy()
                    .sendRedirect(
                            request,
                            response,
                            buildErrorRedirectUri(targetUri, exception.getMessage(), registrationId));
        }
    }

    private String buildSuccessRedirectUri(String targetUri, OAuth2LoginResult loginResult, String registrationId) {
        return targetUri + "#accessToken=" + encode(loginResult.accessToken()) + "&tokenType=Bearer" + "&expiresIn="
                + loginResult.accessTokenExpiresIn() + "&memberId=" + loginResult.memberId() + "&newMember="
                + loginResult.newMember() + "&provider=" + encode(registrationId);
    }

    private String buildErrorRedirectUri(String targetUri, String message, String registrationId) {
        return targetUri + "#error=" + encode(message) + "&provider=" + encode(registrationId);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
