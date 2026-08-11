package com.raota.ramenshop.application.service;

import com.raota.ramenshop.presentation.response.RamenShopBasicInfoResponse;
import com.raota.ramenshop.presentation.response.RamenShopResponse;
import com.raota.ramenshop.presentation.response.EventMenuDto;
import com.raota.ramenshop.presentation.response.NormalMenuDto;
import com.raota.ramenshop.domain.model.EventMenu;
import com.raota.ramenshop.domain.model.EventMenus;
import com.raota.ramenshop.domain.model.NormalMenu;
import com.raota.ramenshop.domain.model.NormalMenus;
import com.raota.ramenshop.domain.model.RamenShop;
import com.raota.ramenshop.domain.repository.RamenShopRepository;
import com.raota.ramenshop.application.port.RamenLogQueryPort;
import com.raota.global.file.FileUploader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final RamenLogQueryPort ramenLogQueryPort;
    private final FileUploader fileUploader;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "ramenShopDetail", key = "#shopId.toString()")
    public RamenShopBasicInfoResponse getShopDetail(Long shopId) {
        RamenShop ramenShop = ramenShopRepository.findByIdAndPublishedTrue(shopId)
                .orElseThrow(() -> new IllegalArgumentException("없는 라멘가게 입니다."));

        return RamenShopBasicInfoResponse.from(
                ramenShop,
                fileUploader.getAccessibleUrl(ramenShop.getImageUrl()),
                normalMenusOf(ramenShop).stream()
                        .map(NormalMenuDto::from)
                        .toList(),
                eventMenusOf(ramenShop).stream()
                        .map(EventMenuDto::from)
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
        Page<RamenShopResponse> shops = ramenShopRepository.searchStores(city, district, keyword, tag, pageable);
        List<Long> shopIds = shops.getContent().stream()
                .map(RamenShopResponse::id)
                .toList();
        Map<Long, RamenLogPreview> previewsByShopId = findRamenLogPreviews(shopIds);

        return shops
                .map(store -> new RamenShopResponse(
                        store.id(),
                        store.name(),
                        store.tagLine(),
                        store.region(),
                        store.tags(),
                        fileUploader.getAccessibleUrl(store.thumbnailUrl()),
                        store.visits(),
                        store.viewCount(),
                        previewsByShopId.getOrDefault(store.id(), RamenLogPreview.EMPTY).count(),
                        previewsByShopId.getOrDefault(store.id(), RamenLogPreview.EMPTY).imageUrls()
                ));
    }

    private Map<Long, RamenLogPreview> findRamenLogPreviews(List<Long> shopIds) {
        if (shopIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, RamenLogPreview> result = new LinkedHashMap<>();
        for (RamenLogQueryPort.Preview row : ramenLogQueryPort.findPreviewRowsByShopIds(shopIds)) {
            RamenLogPreview preview = result.computeIfAbsent(
                    row.ramenShopId(),
                    ignored -> new RamenLogPreview(row.ramenLogCount(), new ArrayList<>())
            );
            preview.imageUrls().add(fileUploader.getAccessibleUrl(row.imageUrl()));
        }
        return result;
    }

    private List<NormalMenu> normalMenusOf(RamenShop ramenShop) {
        NormalMenus normalMenus = ramenShop.getNormalMenus();
        return normalMenus == null ? List.of() : normalMenus.getValues();
    }

    private List<EventMenu> eventMenusOf(RamenShop ramenShop) {
        EventMenus eventMenus = ramenShop.getEventMenus();
        return eventMenus == null ? List.of() : eventMenus.getValues();
    }

    private record RamenLogPreview(long count, List<String> imageUrls) {
        private static final RamenLogPreview EMPTY = new RamenLogPreview(0L, List.of());
    }
}
