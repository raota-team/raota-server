package com.raota.mobile.common.cursor;

import java.util.List;
import java.util.function.Function;

/**
 * 목록 조회에서 {@code size + 1}개를 가져와 다음 페이지 유무와 커서를 계산한다.
 *
 * <p>
 * 컨트롤러에서는
 * {@code @RequestParam(required = false) String cursor, @RequestParam(defaultValue =
 * "20") @Min(1) @Max(CursorPage.MAX_SIZE) int size}를 받고, 서비스에서 반환한 페이지를
 * {@code MobileApiResponse.success(page)}로 감싼다.
 * </p>
 *
 * @param items 현재 페이지의 항목
 * @param nextCursor 다음 페이지 요청에 쓸 커서. 마지막 페이지에서는 {@code null}
 * @param hasNext 다음 페이지 존재 여부
 * @param <T> 항목 유형
 */
public record CursorPage<T>(List<T> items, String nextCursor, boolean hasNext) {

    public static final int DEFAULT_SIZE = 20;

    public static final int MAX_SIZE = 50;

    public CursorPage {
        items = List.copyOf(items);
    }

    /** 저장소에서 최대 {@code size + 1}개를 조회한 뒤 마지막으로 포함된 항목의 커서를 만든다. */
    public static <T> CursorPage<T> of(List<T> fetched, int size, Function<T, Cursor> cursorOf) {
        if (size < 1) {
            throw new IllegalArgumentException("size must be positive");
        }
        if (fetched.size() <= size) {
            return new CursorPage<>(fetched, null, false);
        }

        List<T> items = fetched.subList(0, size);
        String nextCursor = cursorOf.apply(items.get(size - 1)).encode();
        return new CursorPage<>(items, nextCursor, true);
    }
}
