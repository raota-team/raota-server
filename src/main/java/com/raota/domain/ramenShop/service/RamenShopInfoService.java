package com.raota.domain.ramenShop.service;

import com.raota.domain.member.repository.BookmarkRepository;
import com.raota.domain.ramenShop.controller.response.RamenShopBasicInfoResponse;
import com.raota.domain.ramenShop.controller.response.StoreSummaryResponse;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class RamenShopInfoService {

    private final RamenShopCacheService ramenShopCacheService;
    private final BookmarkRepository bookmarkRepository;

    @Transactional(readOnly = true)
    public RamenShopBasicInfoResponse getShopDetailInfo(Long shopId, Long memberId) {
        RamenShopBasicInfoResponse cachedResponse = ramenShopCacheService.getShopDetail(shopId);

        boolean isBookmarked = false;
        if (memberId != null) {
            isBookmarked = bookmarkRepository.existsByMemberProfileIdAndRamenShopIdAndIsDeletedFalse(memberId, shopId);
        }

        return cachedResponse.withBookmark(isBookmarked);
    }

    @Transactional(readOnly = true)
    public Page<StoreSummaryResponse> getRamenShopList(String region, String keyword, String tag, Pageable pageable) {
        return ramenShopCacheService.getFirstPageShopList(region, keyword, tag, pageable);
    }
}
