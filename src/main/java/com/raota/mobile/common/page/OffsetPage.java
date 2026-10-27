package com.raota.mobile.common.page;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** 관리자 화면의 번호 페이지 목록이다. 앱 목록은 {@code CursorPage}를 쓴다. */
public record OffsetPage<T>(List<T> items, PageInfo page) {

    public static final int MAX_SIZE = 100;

    public static <S, T> OffsetPage<T> of(Page<S> page, Function<S, T> mapper) {
        return new OffsetPage<>(
                page.getContent().stream().map(mapper).toList(),
                new PageInfo(
                        page.getNumber(),
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages(),
                        page.hasNext(),
                        page.hasPrevious()));
    }

    public record PageInfo(
            int number, int size, long totalElements, int totalPages, boolean hasNext, boolean hasPrevious) {}
}
