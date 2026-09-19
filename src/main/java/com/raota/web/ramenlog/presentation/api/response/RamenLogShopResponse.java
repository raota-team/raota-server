package com.raota.web.ramenlog.presentation.api.response;

public record RamenLogShopResponse(
        Long id,
        String name,
        long logCount
) {
}
