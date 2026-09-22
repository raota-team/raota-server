package com.raota.global.presentation.v2;

/**
 * 커서를 해석할 수 없을 때 발생한다. v2 응답에서는 400 {@code INVALID_CURSOR}로 변환한다.
 */
public class InvalidCursorException extends RuntimeException {

    public InvalidCursorException(String message) {
        super(message);
    }
}
