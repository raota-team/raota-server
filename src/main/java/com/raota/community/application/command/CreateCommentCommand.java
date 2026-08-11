package com.raota.community.application.command;

public record CreateCommentCommand(
        Long postId,
        Long authorId,
        Long parentCommentId,
        String content
) {
}
