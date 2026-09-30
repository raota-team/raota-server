package com.raota.mobile.account.application.port;

/** 모바일 액세스 토큰과 그 유효기간을 제공한다. */
public interface AccessTokenIssuer {

    String issue(Long userId);

    long expiresInSeconds();

}
