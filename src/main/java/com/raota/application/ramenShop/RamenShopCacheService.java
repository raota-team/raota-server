package com.raota.application.ramenShop;

import com.raota.presentation.api.ramenShop.response.RamenShopBasicInfoResponse;
import com.raota.presentation.api.ramenShop.response.RamenShopResponse;
import com.raota.presentation.api.ramenShop.response.EventMenuDto;
import com.raota.presentation.api.ramenShop.response.NormalMenuDto;
import com.raota.domain.ramenShop.model.EventMenu;
import com.raota.domain.ramenShop.model.EventMenus;
import com.raota.domain.ramenShop.model.NormalMenu;
import com.raota.domain.ramenShop.model.NormalMenus;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import com.raota.infrastructure.file.FileUploader;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RamenShopCacheService {

    private final RamenShopRepository ramenShopRepository;
    private final FileUploader fileUploader;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "ramenShopDetail", key = "#shopId.toString()")
    public RamenShopBasicInfoResponse getShopDetail(Long shopId) {
        RamenShop ramenShop = ramenShopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("없는 라멘가게 입니다."));

        return RamenShopBasicInfoResponse.from(
                ramenShop,
                fileUploader.getAccessibleUrl(ramenShop.getImageUrl()),
                normalMenusOf(ramenShop).stream()
                        .map(menu -> NormalMenuDto.from(menu, fileUploader.getAccessibleUrl(menu.getImageUrl())))
                        .toList(),
                eventMenusOf(ramenShop).stream()
                        .map(menu -> EventMenuDto.from(menu, fileUploader.getAccessibleUrl(menu.getImageUrl())))
                        .toList(),
                false
        );
    }

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "ramenShopList",
            key = "T(String).valueOf(#city) + ':' + T(String).valueOf(#district) + ':' + T(String).valueOf(#keyword) + ':' + T(String).valueOf(#tag) + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort.toString()",
            condition = "#pageable.pageNumber == 0"
    )
    public Page<RamenShopResponse> getFirstPageShopList(String city, String district, String keyword, String tag, Pageable pageable) {
        return ramenShopRepository.searchStores(city, district, keyword, tag, pageable)
                .map(store -> new RamenShopResponse(
                        store.id(),
                        store.name(),
                        store.tagLine(),
                        store.region(),
                        store.tags(),
                        fileUploader.getAccessibleUrl(store.thumbnailUrl()),
                        store.visits(),
                        store.viewCount()
                ));
    }

    private List<NormalMenu> normalMenusOf(RamenShop ramenShop) {
        NormalMenus normalMenus = ramenShop.getNormalMenus();
        return normalMenus == null ? List.of() : normalMenus.getValues();
    }

    private List<EventMenu> eventMenusOf(RamenShop ramenShop) {
        EventMenus eventMenus = ramenShop.getEventMenus();
        return eventMenus == null ? List.of() : eventMenus.getValues();
    }
}
