package com.raota.web.account.infrastructure.auth;

import com.raota.global.presentation.common.RequestIdFilter;
import com.raota.global.security.SecurityErrorResponder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class WebSecurityErrorResponder implements SecurityErrorResponder {

    private static final String AUTHENTICATION_REQUIRED_MESSAGE = "인증이 필요합니다.";

    private static final String ACCESS_DENIED_MESSAGE = "접근 권한이 없습니다.";

    private static final RequestMatcher V2_REQUEST_MATCHER = PathPatternRequestMatcher.pathPattern("/api/v2/**");

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(HttpServletRequest request) {
        return true;
    }

    @Override
    public void unauthorized(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        boolean expiredToken = exception instanceof ExpiredJwtAuthenticationException;
        String code = expiredToken ? "TOKEN_EXPIRED" : "UNAUTHORIZED";
        String message = expiredToken && V2_REQUEST_MATCHER.matches(request) ? "액세스 토큰이 만료되었습니다."
                : responseMessage(exception);
        write(request, response, HttpServletResponse.SC_UNAUTHORIZED, code, message);
    }

    @Override
    public void forbidden(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
            throws IOException {
        write(request, response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN", ACCESS_DENIED_MESSAGE);
    }

    private String responseMessage(AuthenticationException exception) {
        if (exception instanceof JwtAuthenticationException && exception.getMessage() != null
                && !exception.getMessage().isBlank()) {
            return exception.getMessage();
        }
        return AUTHENTICATION_REQUIRED_MESSAGE;
    }

    private void write(HttpServletRequest request, HttpServletResponse response, int status, String v2Code,
            String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Object body = V2_REQUEST_MATCHER.matches(request)
                ? new MobileSecurityErrorBody(false, null, new MobileSecurityError(v2Code, message, List.of()),
                        new MobileSecurityMeta(RequestIdFilter.currentRequestId(request)))
                : new SecurityErrorBody("FAIL", message, false);
        objectMapper.writeValue(response.getWriter(), body);
    }

    private record SecurityErrorBody(String status, String message, boolean success) {
    }

    private record MobileSecurityErrorBody(boolean success, Object data, MobileSecurityError error,
            MobileSecurityMeta meta) {
    }

    private record MobileSecurityError(String code, String message, List<String> fields) {
    }

    private record MobileSecurityMeta(String requestId) {
    }

}
