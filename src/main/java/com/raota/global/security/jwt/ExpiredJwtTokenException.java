package com.raota.global.security.jwt;

public class ExpiredJwtTokenException extends JwtTokenException {

    public ExpiredJwtTokenException(Throwable cause) {
        super(cause);
    }

}
