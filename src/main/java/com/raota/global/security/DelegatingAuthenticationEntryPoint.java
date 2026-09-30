package com.raota.global.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DelegatingAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final List<SecurityErrorResponder> responders;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        responders.stream()
            .filter(responder -> responder.supports(request))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("인증 오류 응답기가 없습니다."))
            .unauthorized(request, response, exception);
    }

}
