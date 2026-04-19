package com.raota.global.auth;

import com.raota.domain.auth.service.AuthService;
import com.raota.domain.auth.service.OAuth2LoginResult;
import com.raota.domain.auth.service.OAuth2UserInfo;
import com.raota.domain.auth.service.OAuth2UserInfoFactory;
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
    private final com.raota.global.auth.repository.HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;
    private final AuthProperties authProperties;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2AuthenticationToken oauth2Authentication = (OAuth2AuthenticationToken) authentication;
        OAuth2User principal = oauth2Authentication.getPrincipal();

        String registrationId = oauth2Authentication.getAuthorizedClientRegistrationId();
        OAuth2UserInfo userInfo = oAuth2UserInfoFactory.from(
                registrationId,
                principal.getAttributes()
        );

        OAuth2LoginResult loginResult = authService.login(userInfo);

        response.addHeader("Set-Cookie", refreshTokenCookieManager.createRefreshTokenCookie(loginResult.refreshToken()).toString());

        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);

        getRedirectStrategy().sendRedirect(request, response, buildSuccessRedirectUri(loginResult, registrationId));
    }

    private String buildSuccessRedirectUri(OAuth2LoginResult loginResult, String registrationId) {
        return authProperties.oauth2().redirectUri()
                + "#accessToken=" + encode(loginResult.accessToken())
                + "&tokenType=Bearer"
                + "&expiresIn=" + loginResult.accessTokenExpiresIn()
                + "&memberId=" + loginResult.memberId()
                + "&newMember=" + loginResult.newMember()
                + "&provider=" + encode(registrationId);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
