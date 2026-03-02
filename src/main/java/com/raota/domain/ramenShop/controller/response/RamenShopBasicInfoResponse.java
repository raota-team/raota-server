package com.raota.domain.ramenShop.controller.response;

import com.raota.domain.ramenShop.dto.BusinessHoursDto;
import com.raota.domain.ramenShop.dto.EventMenuDto;
import com.raota.domain.ramenShop.dto.NormalMenuDto;
import com.raota.domain.ramenShop.dto.ShopStatDto;
import com.raota.domain.ramenShop.model.RamenShop;
import java.util.List;
import lombok.Builder;

@Builder
public record RamenShopBasicInfoResponse(
        Long id,
        String name,
        String image_url,
        String address,
        String instagram_url,
        String catchTableUrl,
        BusinessHoursDto business_hours,
        ShopStatDto stats,
        List<String> tags,
        List<NormalMenuDto> normal_menus,
        List<EventMenuDto> event_menus) {

    public static RamenShopBasicInfoResponse from(RamenShop ramenShop){
        return new RamenShopBasicInfoResponse(
                ramenShop.getId(),
                ramenShop.getName(),
                ramenShop.getImageUrl(),
                ramenShop.getAddress().fullAddress(),
                ramenShop.getInstagramUrl(),
                null,
                BusinessHoursDto.from(ramenShop.getBusinessHours()),
                ShopStatDto.from(ramenShop.getStats()),
                ramenShop.getTags(),
                ramenShop.getNormalMenus().getNormalMenusInfo(),
                ramenShop.getEventMenus().getEventMenusInfo()
        );
    }
}
