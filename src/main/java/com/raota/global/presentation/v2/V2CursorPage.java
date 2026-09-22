package com.raota.global.presentation.v2;

import java.util.List;

/**
 * 커서 기반 목록 응답. {@code nextCursor}는 다음 페이지가 없으면 {@code null}이다.
 */
public record V2CursorPage<T>(List<T> items, String nextCursor, boolean hasNext) {

    public static <T> V2CursorPage<T> of(List<T> items, String nextCursor) {
        return new V2CursorPage<>(List.copyOf(items), nextCursor, nextCursor != null);
    }
}
