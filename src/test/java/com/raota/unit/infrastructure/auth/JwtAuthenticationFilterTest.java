package com.raota.unit.infrastructure.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.raota.application.member.MemberProvisioningService;
import com.raota.infrastructure.auth.AuthenticatedMember;
import com.raota.infrastructure.auth.JwtAuthenticationException;
import com.raota.infrastructure.auth.JwtAuthenticationFilter;
import com.raota.infrastructure.auth.JwtTokenProvider;
import com.raota.infrastructure.auth.RestAuthenticationEntryPoint;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationFilterTest {

    @Mock
    JwtTokenProvider jwtTokenProvider;
    @Mock
    RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    @Mock
    MemberProvisioningService memberProvisioningService;
    @InjectMocks
    JwtAuthenticationFilter jwtAuthenticationFilter;

    MockHttpServletRequest request;
    MockHttpServletResponse response;
    FilterChain filterChain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @Test
    void 유효한_bearer_토큰이_들어온_경우() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer valid-token-string");
        given(jwtTokenProvider.getAuthenticatedMember("valid-token-string")).willReturn(AuthenticatedMember.user(1L));
        given(memberProvisioningService.isActiveMember(1L)).willReturn(true);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isInstanceOf(AuthenticatedMember.class);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void 헤더에_토큰이_없는_경우() throws ServletException, IOException {
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        assertThat(auth).isNull();

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void Bearer_형식이_아닌_경우() throws ServletException, IOException {
        request.addHeader("Authorization", "Bear valid-token-string");
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        assertThat(auth).isNull();

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void 유효하지_않은_토큰인_경우() throws ServletException, IOException {
        String invalidToken = "invalid-token";
        request.addHeader("Authorization", "Bearer " + invalidToken);
        given(jwtTokenProvider.getAuthenticatedMember(invalidToken))
                .willThrow(new JwtAuthenticationException("유효하지 않은 토큰입니다.", new RuntimeException()));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(restAuthenticationEntryPoint).commence(any(), any(), any());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void 탈퇴한_회원의_토큰인_경우() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer withdrawn-token");
        given(jwtTokenProvider.getAuthenticatedMember("withdrawn-token")).willReturn(AuthenticatedMember.user(1L));
        given(memberProvisioningService.isActiveMember(1L)).willReturn(false);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(restAuthenticationEntryPoint).commence(any(), any(), any());
        verify(filterChain, never()).doFilter(request, response);
    }
}
