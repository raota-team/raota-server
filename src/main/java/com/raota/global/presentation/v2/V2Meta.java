package com.raota.global.presentation.v2;

/**
 * 모든 v2 응답에 담기는 메타 정보. {@code requestId}는 응답 헤더 {@code X-Request-Id}와 같은 값이다.
 */
public record V2Meta(String requestId) {
}
