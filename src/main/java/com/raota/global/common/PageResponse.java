package com.raota.global.common;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

@Schema(description = "목록 + 페이지 메타 응답")
public record PageResponse<T>(
        @Schema(description = "목록 데이터")
        List<T> items,
        @Schema(description = "페이지 메타 정보")
        PageMeta page
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), PageMeta.from(page));
    }

    public record PageMeta(
            @Schema(description = "현재 페이지 번호 (0부터 시작)")
            int number,
            @Schema(description = "페이지 크기")
            int size,
            @Schema(description = "전체 항목 수")
            long totalElements,
            @Schema(description = "전체 페이지 수")
            int totalPages,
            @Schema(description = "다음 페이지 존재 여부")
            boolean hasNext,
            @Schema(description = "이전 페이지 존재 여부")
            boolean hasPrevious
    ) {
        private static <T> PageMeta from(Page<T> page) {
            return new PageMeta(
                    page.getNumber(),
                    page.getSize(),
                    page.getTotalElements(),
                    page.getTotalPages(),
                    page.hasNext(),
                    page.hasPrevious()
            );
        }
    }
}
