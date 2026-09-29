package com.raota.web.account.infrastructure.auth;

import com.raota.global.security.BearerTokenAuthenticationException;

public class JwtAuthenticationException extends BearerTokenAuthenticationException {

    public JwtAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

}
