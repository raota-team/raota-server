package com.raota.web.community.application.result;

import java.time.LocalDateTime;

public record HomePostResult(
        Long id,
        String title,
        String contentSnippet,
        AuthorSummary author,
        long commentCount,
        Integer viewCount,
        LocalDateTime createdAt
) {
    public record AuthorSummary(
            String nickname,
            String profileImageUrl
    ) {
    }
}
