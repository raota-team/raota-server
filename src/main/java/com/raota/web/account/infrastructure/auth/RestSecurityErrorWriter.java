package com.raota.web.account.infrastructure.auth;

import com.raota.global.presentation.common.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class RestSecurityErrorWriter {

    private static final RequestMatcher V2_REQUEST_MATCHER = PathPatternRequestMatcher.pathPattern("/api/v2/**");

    private final ObjectMapper objectMapper;

    public void write(HttpServletRequest request, HttpServletResponse response, int status, String v2Code,
            String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Object body = isV2Request(request)
                ? new MobileSecurityErrorBody(false, null, new MobileSecurityError(v2Code, message, List.of()),
                        new MobileSecurityMeta(RequestIdFilter.currentRequestId(request)))
                : new SecurityErrorBody("FAIL", message, false);
        objectMapper.writeValue(response.getWriter(), body);
    }

    static boolean isV2Request(HttpServletRequest request) {
        return V2_REQUEST_MATCHER.matches(request);
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
