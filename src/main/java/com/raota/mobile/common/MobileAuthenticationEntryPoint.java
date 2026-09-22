package com.raota.mobile.common;

import com.raota.global.presentation.v2.V2ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 인증이 없거나 무효한 `/api/v2` 요청에 401을 v2 형식으로 응답한다.
 */
@Component
@RequiredArgsConstructor
public class MobileAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final MobileSecurityErrorWriter errorWriter;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authenticationException
    ) throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        errorWriter.write(response, V2ErrorCode.UNAUTHORIZED, "인증이 필요합니다.");
    }
}
