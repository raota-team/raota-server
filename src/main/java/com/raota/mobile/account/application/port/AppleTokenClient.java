package com.raota.mobile.account.application.port;

/** Apple 인가 코드로 갱신 토큰을 얻고 탈퇴 시 제공자 측 토큰을 폐기한다. */
public interface AppleTokenClient {

    String exchange(String authorizationCode, String expectedSubject);

    /**
     * Apple 쪽 갱신 토큰을 폐기한다. Apple이 4xx로 거절하면 {@link AppleTokenRevocationRejectedException}을 던지고, 5xx·연결 실패는 다른
     * 런타임 예외로 던진다.
     */
    void revoke(String refreshToken);
}
