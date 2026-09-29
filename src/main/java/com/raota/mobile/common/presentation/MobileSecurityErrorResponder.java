package com.raota.mobile.common.presentation;

import com.raota.global.presentation.common.RequestIdFilter;
import com.raota.global.security.BearerTokenAuthenticationException;
import com.raota.global.security.SecurityErrorResponder;
import com.raota.global.security.jwt.ExpiredJwtTokenException;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.presentation.response.MobileApiResponse;
import com.raota.mobile.common.presentation.response.MobileError;
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

/** 모바일 API가 보안 필터에서 거절한 요청을 v2 응답 형식으로 기록한다. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class MobileSecurityErrorResponder implements SecurityErrorResponder {

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(HttpServletRequest request) {
        return MobileApiPath.matches(request);
    }

    @Override
    public void unauthorized(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ExpiredJwtTokenException) {
                write(request, response, MobileErrorCode.TOKEN_EXPIRED, "액세스 토큰이 만료되었습니다.");
                return;
            }
        }
        String message = exception instanceof BearerTokenAuthenticationException && exception.getMessage() != null
                && !exception.getMessage().isBlank() ? exception.getMessage() : "인증이 필요합니다.";
        write(request, response, MobileErrorCode.UNAUTHORIZED, message);
    }

    @Override
    public void forbidden(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
            throws IOException {
        write(request, response, MobileErrorCode.FORBIDDEN, "접근 권한이 없습니다.");
    }

    private void write(HttpServletRequest request, HttpServletResponse response, MobileErrorCode code, String message)
            throws IOException {
        response.setStatus(code.httpStatus().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), MobileApiResponse.failure(MobileError.of(code, message))
            .withRequestId(RequestIdFilter.currentRequestId(request)));
    }

}
