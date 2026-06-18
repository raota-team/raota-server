package com.raota.presentation.api.ramenlog.response;

public record RamenLogShopResponse(
        Long id,
        String name,
        long logCount
) {
}
