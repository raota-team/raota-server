package com.raota.presentation.api.community.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record CommunityCommentItemResponse(
        @Schema(description = "댓글 ID")
        Long commentId,
        @Schema(description = "부모 댓글 ID(답글인 경우)")
        Long parentCommentId,
        @Schema(description = "게시글 ID")
        Long postId,
        @Schema(description = "작성자 닉네임")
        String authorNickname,
        @Schema(description = "작성자 ID")
        Long authorId,
        @Schema(description = "작성자 프로필 이미지 URL")
        String authorImageUrl,
        @Schema(description = "부모 댓글 작성자 닉네임 태그(답글인 경우)")
        String taggedParentAuthorNickname,
        @Schema(description = "작성 일시")
        LocalDateTime createdAt,
        @Schema(description = "댓글 내용")
        String content,
        @Schema(description = "삭제 여부")
        Boolean isDeleted
) {
}
