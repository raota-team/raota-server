package com.raota.application.ramenShop;

import com.raota.domain.member.repository.BookmarkRepository;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import com.raota.infrastructure.cache.CacheInvalidationPublisher;
import com.raota.presentation.api.ramenShop.response.RamenShopResponse;
import com.raota.presentation.api.ramenShop.request.RamenShopSortType;
import com.raota.presentation.api.ramenShop.response.RamenShopBasicInfoResponse;
import com.raota.domain.ramenlog.repository.RamenLogRepository;
import com.raota.presentation.api.ramenShop.response.RecentVerifiedShopResponse;
import java.util.List;
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
    private final RamenShopRepository ramenShopRepository;
    private final BookmarkRepository bookmarkRepository;
    private final RamenLogRepository ramenLogRepository;
    private final CacheInvalidationPublisher cacheInvalidationPublisher;
    private final RamenShopViewRankingService ramenShopViewRankingService;

    @Transactional(readOnly = true)
    public List<RecentVerifiedShopResponse> getRecentVerifiedShops(int limit) {
        return ramenLogRepository.findRecentVerifiedShops(PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public RamenShopBasicInfoResponse getShopDetailInfo(Long shopId, Long memberId) {
        RamenShop ramenShop = ramenShopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("없는 라멘가게 입니다."));
        int viewCount = ramenShop.getStats().viewCount();

        RamenShopBasicInfoResponse cachedResponse = ramenShopCacheService.getShopDetail(shopId);

        boolean isBookmarked = false;
        if (memberId != null) {
            isBookmarked = bookmarkRepository.existsByMemberProfileIdAndRamenShopIdAndIsDeletedFalse(memberId, shopId);
        }

        return cachedResponse.withBookmark(isBookmarked).withViewCount(viewCount);
    }

    @Transactional
    public void increaseViewCount(Long shopId) {
        RamenShop ramenShop = ramenShopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("없는 라멘가게 입니다."));
        ramenShop.increaseViewCount();
        ramenShopViewRankingService.increaseTodayViewCount(shopId);

        cacheInvalidationPublisher.publish("ramenShopDetail", String.valueOf(shopId));
        cacheInvalidationPublisher.publishAll("ramenShopList");
    }

    @Transactional(readOnly = true)
    public Page<RamenShopResponse> getRamenShopList(
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
