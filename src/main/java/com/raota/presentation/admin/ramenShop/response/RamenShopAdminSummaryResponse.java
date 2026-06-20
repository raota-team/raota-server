package com.raota.presentation.admin.ramenShop.response;

public record RamenShopAdminSummaryResponse(
        Long id,
        String name,
        String address,
        String imageUrl,
        boolean published
) {
}
