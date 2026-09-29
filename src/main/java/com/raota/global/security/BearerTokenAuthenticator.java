package com.raota.global.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

public interface BearerTokenAuthenticator {

    boolean supports(HttpServletRequest request);

    Authentication authenticate(String token);

}
