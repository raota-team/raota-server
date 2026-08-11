package com.raota.ramenshop.presentation.admin.response;

public record RamenShopVisibilityBulkUpdateResponse(
        long fromId,
        long toId,
        boolean published,
        int updatedCount
) {
}
