package com.raota.domain.member.controller.response;

import java.time.LocalDateTime;

public record MyPostSummaryResponse(
        Long postId,
        String category,
        String store_name,
        String title,
        LocalDateTime created_at,
        Long like_count,
        Long comment_count
) {
}
