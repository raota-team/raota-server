package com.raota.global.presentation.v2;

/**
 * 입력 검증 실패 항목. 앱이 어떤 입력 칸에 오류를 표시할지 결정한다.
 */
public record V2FieldError(String field, String code, String message) {
}
