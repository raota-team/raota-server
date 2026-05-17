package com.raota.presentation.api.community.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record CommunityPostCardResponse(
        @Schema(description = "글 ID")
        Long postId,
        @Schema(description = "글 카테고리")
        String category,
        @Schema(description = "맛집후기 카테고리일 때만 라멘집 ID, 그 외에는 null")
        Long ramenShopId,
        @Schema(description = "맛집후기 카테고리일 때만 가게 이름, 그 외에는 null")
        String storeName,
        @Schema(description = "글 제목")
        String title,
        @Schema(description = "글 내용 미리보기")
        String contentPreview,
        @Schema(description = "글 이미지 URL")
        String imageUrl,
        @Schema(description = "작성자 이름")
        String authorName,
        @Schema(description = "작성자 ID")
        Long authorId,
        @Schema(description = "작성자 프로필 이미지 URL")
        String authorImageUrl,
        @Schema(description = "작성 일시")
        LocalDateTime createdAt,
        @Schema(description = "좋아요 수")
        Long likeCount,
        @Schema(description = "댓글 수")
        Long commentCount
) {
}
