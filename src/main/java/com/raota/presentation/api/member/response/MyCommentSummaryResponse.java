package com.raota.presentation.api.member.response;

public record MyCommentSummaryResponse(
        Long postId,
        String content,
        String post_title,
        java.time.LocalDateTime post_created_at
) {
}
