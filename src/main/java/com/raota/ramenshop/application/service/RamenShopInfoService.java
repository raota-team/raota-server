package com.raota.ramenshop.application.service;

import com.raota.ramenshop.domain.repository.BookmarkRepository;
import com.raota.ramenshop.domain.model.EventMenus;
import com.raota.ramenshop.domain.model.NormalMenus;
import com.raota.ramenshop.domain.model.RamenShop;
import com.raota.ramenshop.domain.repository.RamenShopRepository;
import com.raota.ramenshop.application.port.RamenLogQueryPort;
import com.raota.global.cache.CacheInvalidationPublisher;
import com.raota.ramenshop.presentation.response.RamenShopMenuOptionsResponse;
import com.raota.ramenshop.presentation.response.RamenShopResponse;
import com.raota.ramenshop.presentation.request.RamenShopSortType;
import com.raota.ramenshop.presentation.response.RamenShopBasicInfoResponse;
import com.raota.ramenshop.presentation.response.RecentVerifiedShopResponse;
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
    private final RamenLogQueryPort ramenLogQueryPort;
    private final CacheInvalidationPublisher cacheInvalidationPublisher;
    private final RamenShopViewRankingService ramenShopViewRankingService;

    @Transactional(readOnly = true)
    public List<RecentVerifiedShopResponse> getRecentVerifiedShops(int limit) {
        return ramenLogQueryPort.findRecentVerifiedShops(limit).stream()
                .map(shop -> new RecentVerifiedShopResponse(
                        shop.id(),
                        shop.name(),
                        shop.location(),
                        shop.imageUrl(),
                        shop.photoCount()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public RamenShopBasicInfoResponse getShopDetailInfo(Long shopId, Long memberId) {
        RamenShop ramenShop = ramenShopRepository.findByIdAndPublishedTrue(shopId)
                .orElseThrow(() -> new IllegalArgumentException("없는 라멘가게 입니다."));
        int viewCount = ramenShop.getStats().viewCount();

        RamenShopBasicInfoResponse cachedResponse = ramenShopCacheService.getShopDetail(shopId);

        boolean isBookmarked = false;
        if (memberId != null) {
            isBookmarked = bookmarkRepository.existsByMemberProfileIdAndRamenShopIdAndIsDeletedFalse(memberId, shopId);
        }

        return cachedResponse.withBookmark(isBookmarked).withViewCount(viewCount);
    }

    @Transactional(readOnly = true)
    public RamenShopMenuOptionsResponse getShopMenuOptions(Long shopId) {
        RamenShop ramenShop = ramenShopRepository.findByIdAndPublishedTrue(shopId)
                .orElseThrow(() -> new IllegalArgumentException("없는 라멘가게 입니다."));
        NormalMenus normalMenus = ramenShop.getNormalMenus();
        EventMenus eventMenus = ramenShop.getEventMenus();

        return RamenShopMenuOptionsResponse.from(
                ramenShop,
                normalMenus == null ? List.of() : normalMenus.getValues(),
                eventMenus == null ? List.of() : eventMenus.getValues()
        );
    }

    @Transactional
    public void increaseViewCount(Long shopId) {
        RamenShop ramenShop = ramenShopRepository.findByIdAndPublishedTrue(shopId)
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
