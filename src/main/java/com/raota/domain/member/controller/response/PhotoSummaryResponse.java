package com.raota.domain.member.controller.response;

import java.time.LocalDateTime;

public record PhotoSummaryResponse(
        Long photo_id,
        String image_url,
        String menuName,
        Long restaurant_id,
        String restaurant_name,
        LocalDateTime uploaded_at
) {
}
