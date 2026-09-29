package com.raota.web.account.infrastructure.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.raota.global.presentation.common.RequestIdFilter;
import com.raota.global.security.DelegatingAccessDeniedHandler;
import com.raota.global.security.DelegatingAuthenticationEntryPoint;
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

class WebSecurityErrorResponderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final WebSecurityErrorResponder responder = new WebSecurityErrorResponder(objectMapper);

    private final DelegatingAuthenticationEntryPoint entryPoint = new DelegatingAuthenticationEntryPoint(
            List.of(responder));

    private final DelegatingAccessDeniedHandler deniedHandler = new DelegatingAccessDeniedHandler(List.of(responder));

    @Test
    void 인증되지_않은_요청은_기존_401_JSON을_반환한다() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(new MockHttpServletRequest(), response,
                new InsufficientAuthenticationException("내부 예외 메시지"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
        assertErrorResponse(response, "인증이 필요합니다.");
    }

    @Test
    void 권한이_부족한_요청은_기존_403_JSON을_반환한다() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        deniedHandler.handle(new MockHttpServletRequest(), response, new AccessDeniedException("내부 예외 메시지"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isNull();
        assertErrorResponse(response, "접근 권한이 없습니다.");
    }

    @Test
    void v2_권한이_부족한_요청은_v2_403_JSON과_requestId를_반환한다() throws Exception {
        MockHttpServletRequest request = mobileRequest("/api/v2/anything");
        request.setAttribute(RequestIdFilter.ATTRIBUTE, "request-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        deniedHandler.handle(request, response, new AccessDeniedException("내부 예외 메시지"));

        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(body.get("success").booleanValue()).isFalse();
        assertThat(body.get("data").isNull()).isTrue();
        assertThat(body.get("error").get("code").stringValue()).isEqualTo("FORBIDDEN");
        assertThat(body.get("error").get("message").stringValue()).isEqualTo("접근 권한이 없습니다.");
        assertThat(body.get("error").get("fields").isEmpty()).isTrue();
        assertThat(body.get("meta").get("requestId").stringValue()).isEqualTo("request-123");
    }

    @Test
    void v2_루트_경로도_v2_JSON을_반환한다() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        deniedHandler.handle(mobileRequest("/api/v2"), response, new AccessDeniedException("내부 예외 메시지"));

        assertThat(objectMapper.readTree(response.getContentAsByteArray()).get("error").get("code").stringValue())
            .isEqualTo("FORBIDDEN");
    }

    @Test
    void v2_만료된_토큰은_TOKEN_EXPIRED를_반환한다() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(mobileRequest("/api/v2/anything"), response,
                new ExpiredJwtAuthenticationException(new IllegalStateException()));

        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(body.get("error").get("code").stringValue()).isEqualTo("TOKEN_EXPIRED");
        assertThat(body.get("error").get("message").stringValue()).isEqualTo("액세스 토큰이 만료되었습니다.");
    }

    @Test
    void v1_만료된_토큰은_기존_메시지를_유지한다() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(new MockHttpServletRequest(), response,
                new ExpiredJwtAuthenticationException(new IllegalStateException()));

        assertErrorResponse(response, "유효하지 않은 액세스 토큰입니다.");
    }

    @Test
    void 서버가_판정한_인증_실패_메시지는_보존한다() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(new MockHttpServletRequest(), response,
                new JwtAuthenticationException("탈퇴 처리된 계정입니다.", new IllegalStateException()));

        assertErrorResponse(response, "탈퇴 처리된 계정입니다.");
    }

    private MockHttpServletRequest mobileRequest(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        return request;
    }

    private void assertErrorResponse(MockHttpServletResponse response, String expectedMessage) throws Exception {
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getCharacterEncoding()).isEqualTo(StandardCharsets.UTF_8.name());
        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(body.get("status").stringValue()).isEqualTo("FAIL");
        assertThat(body.get("message").stringValue()).isEqualTo(expectedMessage);
        assertThat(body.get("success").booleanValue()).isFalse();
    }

}
