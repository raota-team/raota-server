package com.raota.web.account.infrastructure.auth;

import com.raota.web.account.infrastructure.auth.repository.HttpCookieOAuth2AuthorizationRequestRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 소셜 로그인 뒤 돌아갈 주소를 고른다.
 *
 * <p>성공 리다이렉트는 URL fragment에 access token을 싣는다. 그래서 로그인 시작 때 받은 {@code redirect_uri}는 출처가
 * {@code app.auth.oauth2.allowed-redirect-origins}에 있을 때만 쓰고, 아니면 설정된 기본 주소로 보낸다. CORS 허용 목록과 섞지 않는다.
 */
@Component
@RequiredArgsConstructor
public class OAuth2RedirectTargetResolver {

    private final AuthProperties authProperties;

    public String resolve(HttpServletRequest request, String fallbackUri) {
        return requestedUri(request).filter(this::isAllowed).orElse(fallbackUri);
    }

    private Optional<String> requestedUri(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME.equals(
                        cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private boolean isAllowed(String uri) {
        return originOf(uri)
                .map(origin -> authProperties.oauth2().allowedRedirectOrigins().stream()
                        .anyMatch(origin::equalsIgnoreCase))
                .orElse(false);
    }

    private static Optional<String> originOf(String value) {
        URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException exception) {
            return Optional.empty();
        }
        // user-info가 붙은 주소(https://허용도메인@다른호스트)는 출처 비교를 헷갈리게 하므로 거부한다.
        if (uri.getScheme() == null || uri.getHost() == null || uri.getRawUserInfo() != null) {
            return Optional.empty();
        }
        String origin = uri.getScheme() + "://" + uri.getHost();
        if (uri.getPort() != -1) {
            origin = origin + ":" + uri.getPort();
        }
        return Optional.of(origin);
    }
}
