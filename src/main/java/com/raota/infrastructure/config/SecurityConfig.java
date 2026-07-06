package com.raota.infrastructure.config;

import com.raota.infrastructure.auth.AuthProperties;
import com.raota.infrastructure.auth.JwtAuthenticationFilter;
import com.raota.infrastructure.auth.OAuth2AuthenticationFailureHandler;
import com.raota.infrastructure.auth.OAuth2AuthenticationSuccessHandler;
import com.raota.infrastructure.auth.RestAccessDeniedHandler;
import com.raota.infrastructure.auth.RestAuthenticationEntryPoint;
import com.raota.infrastructure.auth.repository.HttpCookieOAuth2AuthorizationRequestRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;
    private final AuthProperties authProperties;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler))
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorization -> authorization
                                .baseUri("/oauth2/authorization")
                                .authorizationRequestRepository(httpCookieOAuth2AuthorizationRequestRepository)) // 쿠키 저장소 등록
                        .redirectionEndpoint(redirection -> redirection
                                .baseUri("/login/oauth2/code/*"))
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/auth/refresh",
                                "/auth/logout",
                                "/error",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers("/users/me").authenticated()
                        .requestMatchers("/users/me/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/community/posts").authenticated()
                        .requestMatchers(HttpMethod.POST, "/community/posts/*/comments").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/community/comments/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/community/comments/*").authenticated()
                        .requestMatchers(HttpMethod.POST, "/ramen-shops/*/photos").authenticated()
                        .requestMatchers(HttpMethod.POST, "/ramen-shops/*/votes/menus/*").authenticated()
                        .requestMatchers(HttpMethod.POST, "/ramen-logs").authenticated()
                        .requestMatchers(HttpMethod.POST, "/ramen-logs/*/likes").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/ramen-logs/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/ramen-logs/*").authenticated()
                        .anyRequest().permitAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> allowedOriginPatterns = new ArrayList<>(List.of(
                "https://*.raota.net",
                "https://raota.net",
                "http://localhost:*",
                "https://localhost:*",
                "http://127.0.0.1:*",
                "https://127.0.0.1:*"
        ));
        allowedOriginPatterns.addAll(authProperties.cors().allowedOrigins());
        configuration.setAllowedOriginPatterns(allowedOriginPatterns.stream().distinct().toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*")); // 모든 헤더 허용
        configuration.setExposedHeaders(List.of("Location", "Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
