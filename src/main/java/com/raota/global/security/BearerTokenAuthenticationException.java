package com.raota.global.security;

import org.springframework.security.core.AuthenticationException;

/** 클라이언트에게 노출해도 안전한 인증 실패 사유를 담는다. */
public class BearerTokenAuthenticationException extends AuthenticationException {

    public BearerTokenAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

}
