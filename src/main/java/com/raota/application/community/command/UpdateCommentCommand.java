package com.raota.application.community.command;

public record UpdateCommentCommand(
        Long commentId,
        Long authorId,
        String content
) {
}
