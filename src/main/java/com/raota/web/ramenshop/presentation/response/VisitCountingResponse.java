package com.raota.web.ramenshop.presentation.response;

public record VisitCountingResponse(Long restaurant_id, Long user_id, int new_visit_count, String message) {
}
