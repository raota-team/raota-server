package com.raota.web.account.infrastructure.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.raota.web.account.infrastructure.auth.repository.HttpCookieOAuth2AuthorizationRequestRepository;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

class OAuth2RedirectTargetResolverTest {

    private static final String DEFAULT_URI = "https://www.raota.net/auth/callback";

    private static final String FAILURE_URI = "https://www.raota.net/auth/error";

    private final AuthProperties authProperties = new AuthProperties(
            "raota",
            "secret",
            1800,
            1209600,
            new AuthProperties.OAuth2(DEFAULT_URI, FAILURE_URI, List.of("https://www.raota.net", "https://raota.net")),
            new AuthProperties.Cookie("raota_refresh_token", true, "None", ".raota.net"),
            // CORS에만 있는 출처는 로그인 리다이렉트 대상이 아니다.
            new AuthProperties.Cors(List.of("http://localhost:3000")));

    private final OAuth2RedirectTargetResolver resolver = new OAuth2RedirectTargetResolver(authProperties);

    @Test
    void 허용된_출처면_요청한_주소로_보낸다() {
        var request = requestWithRedirectUri("https://www.raota.net/auth/callback?next=/my");

        assertThat(resolver.resolve(request, DEFAULT_URI)).isEqualTo("https://www.raota.net/auth/callback?next=/my");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "https://raota.com/steal",
                "http://localhost:3000/auth/callback",
                "http://www.raota.net/auth/callback",
                "https://www.raota.net.evil.com/auth/callback",
                "https://www.raota.net@evil.com/auth/callback",
                "/auth/callback",
                "javascript:alert(1)",
                "https://www.raota.net/ auth"
            })
    void 허용되지_않은_주소면_기본_주소로_보낸다(String requestedUri) {
        var request = requestWithRedirectUri(requestedUri);

        assertThat(resolver.resolve(request, DEFAULT_URI)).isEqualTo(DEFAULT_URI);
    }

    @Test
    void 요청한_주소가_없으면_기본_주소로_보낸다() {
        assertThat(resolver.resolve(new MockHttpServletRequest(), DEFAULT_URI)).isEqualTo(DEFAULT_URI);
    }

    @Test
    void 로그인_실패도_허용되지_않은_주소로_보내지_않는다() throws Exception {
        var handler = new OAuth2AuthenticationFailureHandler(
                authProperties, new HttpCookieOAuth2AuthorizationRequestRepository(), resolver);
        var request = requestWithRedirectUri("https://raota.com/steal");
        request.setRequestURI("/login/oauth2/code/kakao");
        var response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(
                request, response, new OAuth2AuthenticationException(new OAuth2Error("access_denied"), "denied"));

        assertThat(response.getRedirectedUrl())
                .startsWith(FAILURE_URI + "#error=")
                .endsWith("&provider=kakao");
    }

    private static MockHttpServletRequest requestWithRedirectUri(String uri) {
        var request = new MockHttpServletRequest();
        request.setCookies(
                new Cookie(HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME, uri));
        return request;
    }
}
