package com.raota.presentation.admin.ramenShop.dto;

public record RamenShopAdminSummaryResponse(
        Long id,
        String name,
        String address,
        String imageUrl
) {
}
