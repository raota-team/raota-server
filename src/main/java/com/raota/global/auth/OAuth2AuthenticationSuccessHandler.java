package com.raota.global.auth;

import com.raota.domain.auth.service.AuthService;
import com.raota.domain.auth.service.OAuth2LoginResult;
import com.raota.domain.auth.service.OAuth2UserInfo;
import com.raota.domain.auth.service.OAuth2UserInfoFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import static com.raota.global.auth.repository.HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME;

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

        // 쿠키에서 대상 URI 추출, 없으면 기본 설정값 사용
        String targetUri = getTargetUriFromCookie(request);

        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);

        getRedirectStrategy().sendRedirect(request, response, buildSuccessRedirectUri(targetUri, loginResult, registrationId));
    }

    private String getTargetUriFromCookie(HttpServletRequest request) {
        String targetUri = Arrays.stream(request.getCookies() != null ? request.getCookies() : new Cookie[0])
                .filter(cookie -> REDIRECT_URI_PARAM_COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(authProperties.oauth2().redirectUri());

        String requestHost = request.getHeader("Host");
        boolean isProductionHost = requestHost != null && requestHost.contains("raota.net");
        String prodRedirectUri = authProperties.oauth2().redirectUri();

        // 운영 도메인으로 접속했는데 리다이렉트 주소가 localhost인 경우 강제 교정
        if (isProductionHost && targetUri.contains("localhost")) {
            // 발트에 설정된 운영 주소(예: https://www.raota.net)가 있다면 그 도메인을 사용
            if (prodRedirectUri != null && prodRedirectUri.contains("raota.net")) {
                String prodDomain = prodRedirectUri.replaceAll("/+$", ""); // 끝에 슬래시 제거
                return targetUri.replaceFirst("https?://localhost(:[0-9]+)?", prodDomain);
            }
            // 발트 설정이 미비하면 현재 호스트(api.raota.net)라도 사용
            return targetUri.replaceFirst("https?://localhost(:[0-9]+)?", "https://" + requestHost);
        }

        return targetUri;
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

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
