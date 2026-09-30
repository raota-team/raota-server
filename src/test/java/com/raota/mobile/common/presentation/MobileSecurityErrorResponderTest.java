package com.raota.mobile.common.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.raota.global.presentation.common.RequestIdFilter;
import com.raota.global.security.BearerTokenAuthenticationException;
import com.raota.global.security.DelegatingAccessDeniedHandler;
import com.raota.global.security.DelegatingAuthenticationEntryPoint;
import com.raota.global.security.jwt.ExpiredJwtTokenException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class MobileSecurityErrorResponderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final MobileSecurityErrorResponder responder = new MobileSecurityErrorResponder(objectMapper);

    private final DelegatingAuthenticationEntryPoint entryPoint = new DelegatingAuthenticationEntryPoint(
            List.of(responder));

    private final DelegatingAccessDeniedHandler deniedHandler = new DelegatingAccessDeniedHandler(List.of(responder));

    @Test
    void 권한이_부족한_요청은_v2_403_JSON과_requestId를_반환한다() throws Exception {
        MockHttpServletRequest request = mobileRequest("/api/v2/anything");
        request.setAttribute(RequestIdFilter.ATTRIBUTE, "request-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        deniedHandler.handle(request, response, new AccessDeniedException("내부 예외 메시지"));

        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isNull();
        assertThat(response.getCharacterEncoding()).isEqualTo(StandardCharsets.UTF_8.name());
        assertThat(body.get("success").booleanValue()).isFalse();
        assertThat(body.get("data").isNull()).isTrue();
        assertThat(body.get("error").get("code").stringValue()).isEqualTo("FORBIDDEN");
        assertThat(body.get("error").get("message").stringValue()).isEqualTo("접근 권한이 없습니다.");
        assertThat(body.get("error").get("fields").isEmpty()).isTrue();
        assertThat(body.get("meta").get("requestId").stringValue()).isEqualTo("request-123");
    }

    @Test
    void v2_루트와_컨텍스트_경로를_지원하고_비슷한_접두사는_거부한다() throws Exception {
        MockHttpServletRequest request = mobileRequest("/api/v2");
        request.setContextPath("/app");
        request.setRequestURI("/app/api/v2");

        assertThat(responder.supports(request)).isTrue();
        assertThat(responder.supports(mobileRequest("/api/v2extra"))).isFalse();
        assertThat(responder.supports(mobileRequest("/api/v1"))).isFalse();
    }

    @Test
    void 원인_체인에_만료_토큰이_있으면_TOKEN_EXPIRED를_반환한다() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        var exception = new BearerTokenAuthenticationException("유효하지 않은 액세스 토큰입니다.",
                new IllegalStateException(new ExpiredJwtTokenException(new IllegalStateException())));

        entryPoint.commence(mobileRequest("/api/v2/anything"), response, exception);

        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
        assertThat(body.get("error").get("code").stringValue()).isEqualTo("TOKEN_EXPIRED");
        assertThat(body.get("error").get("message").stringValue()).isEqualTo("액세스 토큰이 만료되었습니다.");
    }

    @Test
    void 안전한_인증_메시지는_그대로_반환하고_그_외에는_기본_메시지를_사용한다() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        entryPoint.commence(mobileRequest("/api/v2/anything"), response,
                new BearerTokenAuthenticationException("사용할 수 없는 계정입니다.", null));
        assertThat(objectMapper.readTree(response.getContentAsByteArray()).get("error").get("message").stringValue())
            .isEqualTo("사용할 수 없는 계정입니다.");

        MockHttpServletResponse defaultResponse = new MockHttpServletResponse();
        entryPoint.commence(mobileRequest("/api/v2/anything"), defaultResponse,
                new InsufficientAuthenticationException("내부 예외 메시지"));
        JsonNode body = objectMapper.readTree(defaultResponse.getContentAsByteArray());
        assertThat(body.get("error").get("code").stringValue()).isEqualTo("UNAUTHORIZED");
        assertThat(body.get("error").get("message").stringValue()).isEqualTo("인증이 필요합니다.");
    }

    private MockHttpServletRequest mobileRequest(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        return request;
    }

}
