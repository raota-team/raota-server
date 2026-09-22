package com.raota.mobile.common;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * `/api/v2` 전용 보안 체인.
 *
 * <p>v1 체인보다 먼저 평가되며 v1 JWT 필터를 포함하지 않는다. v2 토큰 검증과 경로별 인가는
 * 인증 이슈(#44)에서 추가한다. 그 전까지는 공개로 지정한 경로만 열고 나머지는 막는다.
 * 새 v2 경로를 만들었는데 인가 규칙을 빠뜨리면 열린 채로 배포되는 대신 403이 된다.</p>
 */
@Configuration
@RequiredArgsConstructor
public class MobileSecurityConfig {

    private final MobileAuthenticationEntryPoint authenticationEntryPoint;
    private final MobileAccessDeniedHandler accessDeniedHandler;

    @Bean
    @Order(1)
    SecurityFilterChain mobileSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(pathPattern("/api/v2/**"))
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(pathPattern(HttpMethod.GET, "/api/v2/ping")).permitAll()
                        .anyRequest().denyAll());

        return http.build();
    }
}
