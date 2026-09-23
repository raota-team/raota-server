package com.raota.mobile.common.presentation.response;

public record MobileApiResponse<T>(boolean success, T data, MobileError error, MobileMeta meta) {

    /** meta는 비워 둔다. 직렬화 직전에 {@link com.raota.mobile.common.presentation.MobileResponseMetaAdvice}가 requestId를 채운다. */
    public static <T> MobileApiResponse<T> success(T data) {
        return new MobileApiResponse<>(true, data, null, null);
    }

    /** meta는 비워 둔다. 직렬화 직전에 {@link com.raota.mobile.common.presentation.MobileResponseMetaAdvice}가 requestId를 채운다. */
    public static <T> MobileApiResponse<T> failure(MobileError error) {
        return new MobileApiResponse<>(false, null, error, null);
    }

    public MobileApiResponse<T> withRequestId(String requestId) {
        return new MobileApiResponse<>(success, data, error, new MobileMeta(requestId));
    }
}
