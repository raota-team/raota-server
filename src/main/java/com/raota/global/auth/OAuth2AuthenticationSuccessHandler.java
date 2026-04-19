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

/**
 * OAuth2 소셜 로그인 성공 시 실행되는 핸들러.
 * Spring Security의 인증 완료 시점과 우리 서비스의 JWT 발급 시점을 잇는 다리 역할을 한다.
 */
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;
    private final OAuth2UserInfoFactory oAuth2UserInfoFactory;
    private final RefreshTokenCookieManager refreshTokenCookieManager;
    private final com.raota.global.auth.repository.HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository; // 추가
    private final AuthProperties authProperties;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        // ... 기존 로직 ...
        OAuth2LoginResult loginResult = authService.login(userInfo);

        // 4. 보안을 위해 Refresh Token은 HttpOnly 쿠키에 담아 응답 헤더에 추가한다.
        response.addHeader("Set-Cookie", refreshTokenCookieManager.createRefreshTokenCookie(loginResult.refreshToken()).toString());

        // 5. 임시 쿠키 삭제
        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);

        // 6. Access Token과 사용자 정보는 프론트엔드 리다이렉트 URL의 Fragment(#)에 실어 보낸다.
        getRedirectStrategy().sendRedirect(request, response, buildSuccessRedirectUri(loginResult, registrationId));
    }

    /**
     * 프론트엔드로 돌아갈 리다이렉트 URL을 구성한다.
     * URL 파라미터 대신 Fragment(#)를 사용하여 보안성과 클라이언트 처리 편의성을 높인다.
     */
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
