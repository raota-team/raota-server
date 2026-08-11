package com.raota.ramenlog.presentation.api.response;

public record RamenLogShopResponse(
        Long id,
        String name,
        long logCount
) {
}
