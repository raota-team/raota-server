package com.raota.mobile.account.application.port;

/** Apple 인가 코드로 갱신 토큰을 얻고 탈퇴 시 제공자 측 토큰을 폐기한다. */
public interface AppleTokenClient {

    String exchange(String authorizationCode, String expectedSubject);

    /** 4xx 거절은 {@link AppleTokenRevocationRejectedException}, 5xx·연결 실패는 그 밖의 런타임 예외를 던진다. */
    void revoke(String refreshToken);
}
