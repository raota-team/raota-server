package com.raota.global.presentation.v2;

/**
 * 모바일 v2 API의 공통 응답 형식.
 *
 * <p>성공이면 {@code data}에 값이 있고 {@code error}가 {@code null}이다. 실패면 반대다.
 * v1의 {@code ApiResponse}와 형식이 다르며 서로 영향을 주지 않는다.</p>
 */
public record V2ApiResponse<T>(boolean success, T data, V2Error error, V2Meta meta) {

    public static <T> V2ApiResponse<T> success(T data, String requestId) {
        return new V2ApiResponse<>(true, data, null, new V2Meta(requestId));
    }

    public static <T> V2ApiResponse<T> failure(V2Error error, String requestId) {
        return new V2ApiResponse<>(false, null, error, new V2Meta(requestId));
    }
}
