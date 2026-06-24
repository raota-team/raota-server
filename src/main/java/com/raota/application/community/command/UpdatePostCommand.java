package com.raota.application.community.command;

public record UpdatePostCommand(
        Long postId,
        String category,
        Long ramenShopId,
        String title,
        String thumbnailUrl,
        String contentFormat,
        String content,
        Long authorId
) {
}