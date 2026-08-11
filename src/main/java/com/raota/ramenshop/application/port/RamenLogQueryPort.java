package com.raota.ramenshop.application.port;

import java.util.Collection;
import java.util.List;

public interface RamenLogQueryPort {

    List<Preview> findPreviewRowsByShopIds(Collection<Long> shopIds);

    List<RecentVerifiedShop> findRecentVerifiedShops(int limit);

    record Preview(Long ramenShopId, String imageUrl, long ramenLogCount) {
    }

    record RecentVerifiedShop(
            Long id,
            String name,
            String location,
            String imageUrl,
            long photoCount
    ) {
    }
}
