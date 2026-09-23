package com.raota.web.account.infrastructure.auth;

public class ExpiredJwtAuthenticationException extends JwtAuthenticationException {

    public ExpiredJwtAuthenticationException(Throwable cause) {
        super("유효하지 않은 액세스 토큰입니다.", cause);
    }

}
