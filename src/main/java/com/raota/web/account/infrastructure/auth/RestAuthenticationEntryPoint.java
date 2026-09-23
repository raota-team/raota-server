package com.raota.web.account.infrastructure.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String AUTHENTICATION_REQUIRED_MESSAGE = "인증이 필요합니다.";
    private static final String EXPIRED_ACCESS_TOKEN_MESSAGE = "액세스 토큰이 만료되었습니다.";

    private final RestSecurityErrorWriter errorWriter;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        boolean expiredToken = authException instanceof ExpiredJwtAuthenticationException;
        String v2Code = expiredToken ? "TOKEN_EXPIRED" : "UNAUTHORIZED";
        String message = expiredToken && RestSecurityErrorWriter.isV2Request(request)
                ? EXPIRED_ACCESS_TOKEN_MESSAGE
                : responseMessage(authException);
        errorWriter.write(
                request,
                response,
                HttpServletResponse.SC_UNAUTHORIZED,
                v2Code,
                message
        );
    }

    private String responseMessage(AuthenticationException authException) {
        if (authException instanceof JwtAuthenticationException
                && authException.getMessage() != null
                && !authException.getMessage().isBlank()) {
            return authException.getMessage();
        }
        return AUTHENTICATION_REQUIRED_MESSAGE;
    }
}
