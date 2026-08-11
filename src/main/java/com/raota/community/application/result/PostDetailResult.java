package com.raota.community.application.result;

import java.time.LocalDateTime;
import java.util.List;

public record PostDetailResult(
        String category,
        String storeName,
        String title,
        String authorName,
        Long authorId,
        String authorImageUrl,
        LocalDateTime createdAt,
        List<String> imageUrls,
        String contentFormat,
        String content,
        Long likeCount,
        Long commentCount,
        Integer viewCount,
        Boolean isLiked
) {
}
