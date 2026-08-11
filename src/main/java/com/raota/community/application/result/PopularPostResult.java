package com.raota.community.application.result;

import java.time.LocalDateTime;

public record PopularPostResult(
        Long postId,
        String category,
        String categoryName,
        String title,
        Long likeCount,
        Long commentCount,
        LocalDateTime createdAt
) {
}
