package com.raota.presentation.api.member.response;

import java.time.LocalDateTime;

public record VisitSummaryResponse(
        Long restaurant_id,
        String restaurant_name,
        String restaurant_image_url,
        String simple_address,
        long visit_count_for_user,
        LocalDateTime last_visited_at
) {
}