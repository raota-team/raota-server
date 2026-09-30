package com.raota.web.account.infrastructure.config;

import com.raota.global.security.SecurityHttpCustomizer;
import com.raota.web.account.infrastructure.auth.OAuth2AuthenticationFailureHandler;
import com.raota.web.account.infrastructure.auth.OAuth2AuthenticationSuccessHandler;
import com.raota.web.account.infrastructure.auth.repository.HttpCookieOAuth2AuthorizationRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSecurityHttpCustomizer implements SecurityHttpCustomizer {

    private final OAuth2AuthenticationSuccessHandler successHandler;

    private final OAuth2AuthenticationFailureHandler failureHandler;

    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @Override
    public void customize(HttpSecurity http) throws Exception {
        http.oauth2Login(oauth2 -> oauth2
            .authorizationEndpoint(authorization -> authorization.baseUri("/oauth2/authorization")
                .authorizationRequestRepository(authorizationRequestRepository))
            .redirectionEndpoint(redirection -> redirection.baseUri("/login/oauth2/code/*"))
            .successHandler(successHandler)
            .failureHandler(failureHandler));
    }

}
