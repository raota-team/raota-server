package com.raota.application.ramenShop;

import com.raota.domain.member.repository.BookmarkRepository;
import com.raota.presentation.api.ramenShop.response.RamenShopResponse;
import com.raota.presentation.api.ramenShop.request.RamenShopSortType;
import com.raota.presentation.api.ramenShop.response.RamenShopBasicInfoResponse;
import com.raota.domain.ramenShop.repository.RamenProofPictureRepository;
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
    private final BookmarkRepository bookmarkRepository;
    private final RamenProofPictureRepository ramenProofPictureRepository;

    @Transactional(readOnly = true)
    public List<RecentVerifiedShopResponse> getRecentVerifiedShops(int limit) {
        return ramenProofPictureRepository.findRecentVerifiedShops(PageRequest.of(0, limit));
    }

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
