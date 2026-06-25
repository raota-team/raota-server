package com.raota.application.community.command;

public record CreateCommentCommand(
        Long postId,
        Long authorId,
        Long parentCommentId,
        String content
) {
}
