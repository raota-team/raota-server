package com.raota.mobile.account.application.port;

/** Apple이 토큰 폐기를 4xx로 거절했다. 같은 요청을 다시 보내도 결과가 같다. */
public class AppleTokenRevocationRejectedException extends RuntimeException {

    public AppleTokenRevocationRejectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
