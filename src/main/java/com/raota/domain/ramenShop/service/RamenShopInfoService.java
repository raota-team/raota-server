package com.raota.domain.ramenShop.service;

import com.raota.domain.ramenShop.controller.response.RamenShopBasicInfoResponse;
import com.raota.domain.ramenShop.controller.response.StoreSummaryResponse;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class RamenShopInfoService {
    private final RamenShopRepository ramenShopRepository;

    public RamenShopBasicInfoResponse getShopDetailInfo(Long shopId) {
        RamenShop ramenShop = ramenShopRepository.findById(shopId).orElseThrow(()-> new IllegalArgumentException("없는 라멘가게 입니다."));
        return RamenShopBasicInfoResponse.from(ramenShop);
    }

    public Page<StoreSummaryResponse> getRamenShopList(String region, String keyword, Pageable pageable) {
        return ramenShopRepository.searchStores(region, keyword, pageable);
    }
}