package com.raota.ramenshop.application.service;

import com.raota.ramenshop.domain.repository.RamenShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RamenShopStatisticsService {

    private final RamenShopRepository ramenShopRepository;

    @Transactional(readOnly = true)
    public long countPublishedShops() {
        return ramenShopRepository.countByPublishedTrue();
    }
}
