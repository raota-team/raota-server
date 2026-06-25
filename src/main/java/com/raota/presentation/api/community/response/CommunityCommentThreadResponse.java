package com.raota.presentation.api.community.response;

import com.raota.application.community.result.CommentThreadResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record CommunityCommentThreadResponse(
        @Schema(description = "댓글 ID")
        Long commentId,
        @Schema(description = "작성자 닉네임")
        String authorNickname,
        @Schema(description = "작성자 ID")
        Long authorId,
        @Schema(description = "작성자 프로필 이미지 URL")
        String authorImageUrl,
        @Schema(description = "작성 일시")
        LocalDateTime createdAt,
        @Schema(description = "댓글 내용")
        String content,
        @Schema(description = "삭제 여부")
        Boolean isDeleted,
        @Schema(description = "답글 목록(depth 1)")
        List<CommunityCommentItemResponse> replies
) {
    public static CommunityCommentThreadResponse from(CommentThreadResult result) {
        return new CommunityCommentThreadResponse(
                result.commentId(),
                result.authorNickname(),
                result.authorId(),
                result.authorImageUrl(),
                result.createdAt(),
                result.content(),
                result.isDeleted(),
                result.replies().stream()
                        .map(CommunityCommentItemResponse::from)
                        .toList()
        );
    }
}
