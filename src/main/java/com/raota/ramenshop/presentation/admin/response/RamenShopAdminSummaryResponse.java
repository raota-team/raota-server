package com.raota.ramenshop.presentation.admin.response;

public record RamenShopAdminSummaryResponse(
        Long id,
        String name,
        String address,
        String imageUrl,
        boolean published
) {
}
