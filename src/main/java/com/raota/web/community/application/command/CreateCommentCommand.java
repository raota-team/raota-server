package com.raota.web.community.application.command;

public record CreateCommentCommand(Long postId, Long authorId, Long parentCommentId, String content) {
}
