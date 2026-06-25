package com.raota.application.community.result;

import java.time.LocalDateTime;
import java.util.List;

public record CommentThreadResult(
        Long commentId,
        String authorNickname,
        Long authorId,
        String authorImageUrl,
        LocalDateTime createdAt,
        String content,
        Boolean isDeleted,
        List<CommentItemResult> replies
) {
    public static CommentThreadResult of(CommentItemResult parent, List<CommentItemResult> replies) {
        return new CommentThreadResult(
                parent.commentId(),
                parent.authorNickname(),
                parent.authorId(),
                parent.authorImageUrl(),
                parent.createdAt(),
                parent.content(),
                parent.isDeleted(),
                replies
        );
    }
}
