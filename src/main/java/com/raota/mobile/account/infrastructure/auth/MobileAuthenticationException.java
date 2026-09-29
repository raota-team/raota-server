package com.raota.mobile.account.infrastructure.auth;

import com.raota.global.security.BearerTokenAuthenticationException;

public class MobileAuthenticationException extends BearerTokenAuthenticationException {

    public MobileAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

}
