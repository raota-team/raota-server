package com.raota.ramenlog.application;

import com.raota.ramenlog.domain.repository.RecentVerifiedShopProjection;
import com.raota.ramenshop.application.port.RamenLogQueryPort;
import com.raota.ramenlog.domain.repository.RamenLogRepository;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RamenLogQueryAdapter implements RamenLogQueryPort {

    private final RamenLogRepository ramenLogRepository;

    @Override
    public List<Preview> findPreviewRowsByShopIds(Collection<Long> shopIds) {
        return ramenLogRepository.findPreviewRowsByShopIds(shopIds).stream()
                .map(row -> new Preview(row.getRamenShopId(), row.getImageUrl(), row.getRamenLogCount()))
                .toList();
    }

    @Override
    public List<RecentVerifiedShop> findRecentVerifiedShops(int limit) {
        return ramenLogRepository.findRecentVerifiedShops(PageRequest.of(0, limit)).stream()
                .map(this::toRecentVerifiedShop)
                .toList();
    }

    private RecentVerifiedShop toRecentVerifiedShop(RecentVerifiedShopProjection response) {
        return new RecentVerifiedShop(
                response.id(),
                response.name(),
                response.location(),
                response.imageUrl(),
                response.photoCount()
        );
    }
}
