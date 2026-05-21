package com.raota.presentation.api.ramenShop.response;

public record VisitCountingResponse(
        Long restaurant_id,
        Long user_id,
        int new_visit_count,
        String message
) {
}
