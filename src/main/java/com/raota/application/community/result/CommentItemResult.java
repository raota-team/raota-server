package com.raota.application.community.result;

import java.time.LocalDateTime;

public record CommentItemResult(
        Long commentId,
        Long parentCommentId,
        Long postId,
        String authorNickname,
        Long authorId,
        String authorImageUrl,
        String taggedParentAuthorNickname,
        LocalDateTime createdAt,
        String content,
        Boolean isDeleted
) {
}
