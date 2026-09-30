package com.raota.web.account.infrastructure.auth;

import com.raota.global.security.SecurityErrorResponder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class WebSecurityErrorResponder implements SecurityErrorResponder {

    private static final String AUTHENTICATION_REQUIRED_MESSAGE = "인증이 필요합니다.";

    private static final String ACCESS_DENIED_MESSAGE = "접근 권한이 없습니다.";

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(HttpServletRequest request) {
        return true;
    }

    @Override
    public void unauthorized(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        write(response, HttpServletResponse.SC_UNAUTHORIZED, responseMessage(exception));
    }

    @Override
    public void forbidden(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
            throws IOException {
        write(response, HttpServletResponse.SC_FORBIDDEN, ACCESS_DENIED_MESSAGE);
    }

    private String responseMessage(AuthenticationException exception) {
        if (exception instanceof JwtAuthenticationException && exception.getMessage() != null
                && !exception.getMessage().isBlank()) {
            return exception.getMessage();
        }
        return AUTHENTICATION_REQUIRED_MESSAGE;
    }

    private void write(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), new SecurityErrorBody("FAIL", message, false));
    }

    private record SecurityErrorBody(String status, String message, boolean success) {
    }

}
