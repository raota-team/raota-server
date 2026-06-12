package com.raota.infrastructure.auth;

public class WithdrawnMemberException extends AuthenticationRequiredException {

    public WithdrawnMemberException(String message) {
        super(message);
    }
}
