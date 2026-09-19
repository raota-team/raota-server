package com.raota.web.community.application.command;

public record UpdateCommentCommand(
        Long commentId,
        Long authorId,
        String content
) {
}
