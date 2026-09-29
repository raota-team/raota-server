package com.raota.global.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DelegatingAccessDeniedHandler implements AccessDeniedHandler {

    private final List<SecurityErrorResponder> responders;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
            throws IOException {
        responders.stream()
            .filter(responder -> responder.supports(request))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("접근 거부 응답기가 없습니다."))
            .forbidden(request, response, exception);
    }

}
