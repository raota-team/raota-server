package com.raota.global.security;

import jakarta.servlet.DispatcherType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter;

    private final DelegatingAuthenticationEntryPoint entryPoint;

    private final DelegatingAccessDeniedHandler accessDeniedHandler;

    private final AccessRuleRegistry accessRuleRegistry;

    private final List<SecurityHttpCustomizer> customizers;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exception -> exception.authenticationEntryPoint(entryPoint)
                .accessDeniedHandler(accessDeniedHandler));

        for (SecurityHttpCustomizer customizer : customizers) {
            customizer.customize(http);
        }

        http.authorizeHttpRequests(auth -> auth.dispatcherTypeMatchers(DispatcherType.ERROR)
            .permitAll()
            .requestMatchers(accessRuleRegistry.matchersFor(AccessLevel.PUBLIC))
            .permitAll()
            .requestMatchers(accessRuleRegistry.matchersFor(AccessLevel.ADMIN))
            .hasRole("ADMIN")
            .requestMatchers(accessRuleRegistry.matchersFor(AccessLevel.AUTHENTICATED))
            .authenticated()
            .anyRequest()
            .authenticated())
            .addFilterBefore(bearerTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** 이 필터는 서블릿 필터로 중복 등록하지 않고 보안 체인 안에서만 실행한다. */
    @Bean
    FilterRegistrationBean<BearerTokenAuthenticationFilter> bearerTokenFilterRegistration(
            BearerTokenAuthenticationFilter filter) {
        FilterRegistrationBean<BearerTokenAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

}
