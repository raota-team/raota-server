package com.raota.account.presentation.member.response;

import java.time.LocalDateTime;

public record BookmarkSummaryResponse(
        Long restaurant_id,
        String restaurant_name,
        String restaurant_image_url,
        String address_simple,
        LocalDateTime bookmarked_at) {
}
