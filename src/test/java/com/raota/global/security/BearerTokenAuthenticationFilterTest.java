package com.raota.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

class BearerTokenAuthenticationFilterTest {

    private final BearerTokenAuthenticator mobile = mock(BearerTokenAuthenticator.class);

    private final BearerTokenAuthenticator web = mock(BearerTokenAuthenticator.class);

    private final DelegatingAuthenticationEntryPoint entryPoint = mock(DelegatingAuthenticationEntryPoint.class);

    private final BearerTokenAuthenticationFilter filter = new BearerTokenAuthenticationFilter(List.of(mobile, web),
            entryPoint);

    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v2/probe");

    private final MockHttpServletResponse response = new MockHttpServletResponse();

    private final FilterChain chain = mock(FilterChain.class);

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 첫_지원_인증기의_결과로_인증한다() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid-token");
        when(mobile.supports(request)).thenReturn(true);
        var authentication = new UsernamePasswordAuthenticationToken("mobile", null, List.of());
        when(mobile.authenticate("valid-token")).thenReturn(authentication);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(authentication);
        verify(web, never()).authenticate("valid-token");
        verify(chain).doFilter(request, response);
    }

    @Test
    void 잘못된_토큰은_다음_인증기나_익명_요청으로_넘기지_않는다() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid");
        when(mobile.supports(request)).thenReturn(true);
        var exception = new BearerTokenAuthenticationException("유효하지 않은 액세스 토큰입니다.", null);
        when(mobile.authenticate("invalid")).thenThrow(exception);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(entryPoint).commence(request, response, exception);
        verify(web, never()).authenticate("invalid");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void 인증기가_지원하지_않으면_다음_인증기를_사용한다() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer legacy");
        when(web.supports(request)).thenReturn(true);
        var authentication = new UsernamePasswordAuthenticationToken("web", null, List.of());
        when(web.authenticate("legacy")).thenReturn(authentication);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(authentication);
    }

    @Test
    void 헤더가_없거나_Bearer가_아니면_검증하지_않는다() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bear malformed");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(mobile, never()).supports(request);
    }

    @Test
    void OPTIONS_요청은_잘못된_토큰도_검증하지_않는다() throws Exception {
        request.setMethod("OPTIONS");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(mobile, never()).supports(request);
    }

}
