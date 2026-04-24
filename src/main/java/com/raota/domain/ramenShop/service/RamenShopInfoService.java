package com.raota.domain.ramenShop.service;

import com.raota.domain.member.repository.BookmarkRepository;
import com.raota.domain.ramenShop.controller.response.RamenShopBasicInfoResponse;
import com.raota.domain.ramenShop.controller.response.StoreSummaryResponse;
import com.raota.domain.ramenShop.dto.EventMenuDto;
import com.raota.domain.ramenShop.dto.NormalMenuDto;
import com.raota.domain.ramenShop.model.EventMenu;
import com.raota.domain.ramenShop.model.NormalMenu;
import com.raota.domain.ramenShop.model.EventMenus;
import com.raota.domain.ramenShop.model.NormalMenus;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import com.raota.global.file.FileUploader;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class RamenShopInfoService {

    private final RamenShopRepository ramenShopRepository;
    private final FileUploader fileUploader;
    private final BookmarkRepository bookmarkRepository;

    @Transactional(readOnly = true)
    public RamenShopBasicInfoResponse getShopDetailInfo(Long shopId,Long memberId) {
        RamenShop ramenShop = ramenShopRepository.findById(shopId).orElseThrow(()-> new IllegalArgumentException("없는 라멘가게 입니다."));

        boolean isBookmarked = false;
        if(memberId != null){
            isBookmarked = bookmarkRepository.existsByMemberProfileIdAndRamenShopIdAndIsDeletedFalse(memberId, shopId);
        }

        return RamenShopBasicInfoResponse.from(
                ramenShop,
                fileUploader.getAccessibleUrl(ramenShop.getImageUrl()),
                normalMenusOf(ramenShop).stream()
                        .map(menu -> NormalMenuDto.from(menu, fileUploader.getAccessibleUrl(menu.getImageUrl())))
                        .toList(),
                eventMenusOf(ramenShop).stream()
                        .map(menu -> EventMenuDto.from(menu, fileUploader.getAccessibleUrl(menu.getImageUrl())))
                        .toList(),
                isBookmarked
        );
    }

    @Transactional(readOnly = true)
    public Page<StoreSummaryResponse> getRamenShopList(String region, String keyword, Pageable pageable) {
        return ramenShopRepository.searchStores(region, keyword, pageable)
                .map(store -> new StoreSummaryResponse(
                        store.id(),
                        store.name(),
                        store.tagLine(),
                        store.region(),
                        store.tags(),
                        fileUploader.getAccessibleUrl(store.thumbnailUrl()),
                        store.visits()
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
