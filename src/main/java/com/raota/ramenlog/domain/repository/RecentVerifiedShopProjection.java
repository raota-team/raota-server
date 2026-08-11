package com.raota.ramenlog.domain.repository;

public record RecentVerifiedShopProjection(
        Long id,
        String name,
        String location,
        String imageUrl,
        long photoCount
) {
}
