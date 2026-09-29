package com.raota.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 요청 경로를 담당하는 인증기를 선택해 Bearer 토큰을 검증한다. */
@Component
@RequiredArgsConstructor
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private final List<BearerTokenAuthenticator> authenticators;

    private final DelegatingAuthenticationEntryPoint entryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            BearerTokenAuthenticator authenticator = authenticators.stream()
                .filter(candidate -> candidate.supports(request))
                .findFirst()
                .orElseThrow(() -> new BearerTokenAuthenticationException("유효하지 않은 액세스 토큰입니다.", null));
            SecurityContextHolder.getContext()
                .setAuthentication(authenticator.authenticate(authorizationHeader.substring(7)));
            filterChain.doFilter(request, response);
        }
        catch (AuthenticationException exception) {
            SecurityContextHolder.clearContext();
            entryPoint.commence(request, response, exception);
        }
    }

}
