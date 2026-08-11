package com.raota.community.application.command;

public record UpdateCommentCommand(
        Long commentId,
        Long authorId,
        String content
) {
}
