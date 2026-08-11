package com.raota.account.infrastructure.auth;

import com.raota.application.member.MemberLifecycleService;
import com.raota.application.member.MemberProvisioningService;
import com.raota.domain.member.model.MemberRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 모든 HTTP 요청에서 JWT 토큰을 검사하는 보안 필터.
 * 요청 헤더의 'Authorization' 값을 읽어 토큰의 유효성을 검증하고, 인증 정보를 설정한다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final MemberProvisioningService memberProvisioningService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        // OPTIONS 요청(Preflight)은 JWT 검증을 건너뛰고 다음 필터로 넘긴다.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1. 요청 헤더에서 Authorization 값을 꺼낸다.
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        // 2. 토큰이 없거나 형식이 'Bearer '로 시작하지 않으면 다음 필터로 바로 넘긴다.
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 3. 토큰에서 실제 JWT 문자열만 추출하여 유효성 검증 및 사용자 정보를 추출한다.
            String token = authorizationHeader.substring(7);
            Long memberId = jwtTokenProvider.getMemberId(token);
            MemberRole role = memberProvisioningService.findActiveMemberRole(memberId)
                    .orElseThrow(() -> new JwtAuthenticationException(
                            MemberLifecycleService.WITHDRAWN_MEMBER_MESSAGE,
                            new IllegalStateException("inactive member")
                    ));
            AuthenticatedMember authenticatedMember = AuthenticatedMember.of(memberId, role);
            
            // 4. Spring Security가 인식할 수 있는 인증 객체(Authentication)를 생성한다.
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    authenticatedMember, // 인증된 사용자 객체 (Principal)
                    null,
                    authenticatedMember.authorities() // 권한 목록
            );
            
            // 5. 생성된 인증 정보를 보안 컨텍스트에 저장하여 요청 처리 동안 유지한다.
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // 6. 다음 필터 체인을 계속 수행한다.
            filterChain.doFilter(request, response);
        } catch (JwtAuthenticationException exception) {
            // 토큰이 유효하지 않은 경우(만료, 조작 등), 인증 정보를 비우고 
            // 미리 정의된 인증 실패 처리기(RestAuthenticationEntryPoint)를 호출한다.
            SecurityContextHolder.clearContext();
            restAuthenticationEntryPoint.commence(request, response, exception);
        }
    }
}
