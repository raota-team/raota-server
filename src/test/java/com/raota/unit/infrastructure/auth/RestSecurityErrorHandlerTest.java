package com.raota.unit.infrastructure.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.raota.infrastructure.auth.RestAccessDeniedHandler;
import com.raota.infrastructure.auth.RestAuthenticationEntryPoint;
import com.raota.infrastructure.auth.RestSecurityErrorWriter;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class RestSecurityErrorHandlerTest {

    private ObjectMapper objectMapper;
    private RestAuthenticationEntryPoint authenticationEntryPoint;
    private RestAccessDeniedHandler accessDeniedHandler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        RestSecurityErrorWriter errorWriter = new RestSecurityErrorWriter(objectMapper);
        authenticationEntryPoint = new RestAuthenticationEntryPoint(errorWriter);
        accessDeniedHandler = new RestAccessDeniedHandler(errorWriter);
    }

    @Test
    void 인증되지_않은_요청은_동일한_401_JSON_계약을_반환한다() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        authenticationEntryPoint.commence(
                new MockHttpServletRequest(),
                response,
                new InsufficientAuthenticationException("내부 예외 메시지")
        );

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
        assertErrorResponse(response, "인증이 필요합니다.");
    }

    @Test
    void 권한이_부족한_요청은_동일한_403_JSON_계약을_반환한다() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(
                new MockHttpServletRequest(),
                response,
                new AccessDeniedException("내부 예외 메시지")
        );

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isNull();
        assertErrorResponse(response, "접근 권한이 없습니다.");
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
