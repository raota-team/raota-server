package com.raota.application.ramenShop;

import com.raota.domain.member.repository.BookmarkRepository;
import com.raota.presentation.api.ramenShop.dto.RamenShopSortType;
import com.raota.presentation.api.ramenShop.dto.RamenShopBasicInfoResponse;
import com.raota.presentation.api.ramenShop.dto.StoreSummaryResponse;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    public Page<StoreSummaryResponse> getRamenShopList(
            String city,
            String district,
            String keyword,
            String tag,
            RamenShopSortType sort,
            Pageable pageable
    ) {
        RamenShopSortType sortType = RamenShopSortType.defaultIfNull(sort);
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sortType.toSort()
        );

        return ramenShopCacheService.getFirstPageShopList(city, district, keyword, tag, sortedPageable);
    }
}
