package com.raota.application.community.result;

import java.time.LocalDateTime;

public record PostCardResult(
        Long postId,
        String category,
        Long ramenShopId,
        String storeName,
        String title,
        String contentPreview,
        String imageUrl,
        String authorName,
        Long authorId,
        String authorImageUrl,
        LocalDateTime createdAt,
        Long likeCount,
        Long commentCount,
        Integer viewCount
) {
}
