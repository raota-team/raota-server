package com.raota.mobile.account.application.port;

/**
 * Apple이 토큰 폐기 요청을 4xx로 거절했다. 같은 요청을 다시 보내도 결과가 바뀌지 않는다(이미 무효인 토큰, 잘못된 클라이언트 설정 등). 5xx나
 * 연결 실패처럼 다시 시도할 수 있는 실패는 이 예외가 아니다.
 */
public class AppleTokenRevocationRejectedException extends RuntimeException {

    public AppleTokenRevocationRejectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
