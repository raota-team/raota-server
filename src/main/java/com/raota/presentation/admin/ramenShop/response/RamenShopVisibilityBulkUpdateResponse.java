package com.raota.presentation.admin.ramenShop.response;

public record RamenShopVisibilityBulkUpdateResponse(
        long fromId,
        long toId,
        boolean published,
        int updatedCount
) {
}
