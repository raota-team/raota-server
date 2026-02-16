package com.raota.domain.community.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record CommunityPostDetailResponse(
        @Schema(description = "글 카테고리")
        String category,
        @Schema(description = "맛집후기 카테고리일 때만 가게 이름, 그 외에는 null")
        String storeName,
        @Schema(description = "글 제목")
        String title,
        @Schema(description = "작성자 이름")
        String authorName,
        @Schema(description = "작성 일시")
        LocalDateTime createdAt,
        @Schema(description = "글 이미지 URL 리스트")
        List<String> imageUrls,
        @Schema(description = "본문 포맷", allowableValues = {"MARKDOWN", "PLAIN"})
        String contentFormat,
        @Schema(description = "글 내용")
        String content,
        @Schema(description = "좋아요 수")
        Long likeCount,
        @Schema(description = "댓글 수")
        Long commentCount
) {
}
