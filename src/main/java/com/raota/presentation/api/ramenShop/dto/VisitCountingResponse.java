package com.raota.presentation.api.ramenShop.dto;

public record VisitCountingResponse(
        Long restaurant_id,
        Long user_id,
        int new_visit_count,
        String message
) {
}
