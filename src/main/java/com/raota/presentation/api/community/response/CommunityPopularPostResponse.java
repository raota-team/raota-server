package com.raota.presentation.api.community.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record CommunityPopularPostResponse(
        @Schema(description = "글 ID")
        Long postId,
        @Schema(description = "원본 글 카테고리 코드", example = "FREE")
        String category,
        @Schema(description = "원본 글 카테고리 표시명", example = "자유게시판")
        String categoryName,
        @Schema(description = "글 제목")
        String title,
        @Schema(description = "좋아요 수")
        Long likeCount,
        @Schema(description = "댓글 수")
        Long commentCount,
        @Schema(description = "작성 일시")
        LocalDateTime createdAt
) {
}
