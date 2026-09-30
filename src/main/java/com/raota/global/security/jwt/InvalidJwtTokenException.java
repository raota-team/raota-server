package com.raota.global.security.jwt;

public class InvalidJwtTokenException extends JwtTokenException {

    public InvalidJwtTokenException(Throwable cause) {
        super(cause);
    }

}
