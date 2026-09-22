package com.raota.global.presentation.v2;

import java.util.List;

/**
 * v2 실패 응답의 오류 본문. {@code fields}는 입력 검증 실패일 때만 값이 있다.
 */
public record V2Error(String code, String message, List<V2FieldError> fields) {

    public static V2Error of(V2ErrorCode code, String message) {
        return new V2Error(code.name(), message, List.of());
    }

    public static V2Error of(V2ErrorCode code, String message, List<V2FieldError> fields) {
        return new V2Error(code.name(), message, List.copyOf(fields));
    }
}
