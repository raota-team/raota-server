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
        String branch_name,
        String naver_map_id,
        String image_url,
        String address,
        String instagram_url,
        String catchTableUrl,
        String description,
        BusinessHoursDto business_hours,
        ShopStatDto stats,
        boolean is_bookmarked,
        List<String> tags,
        List<NormalMenuDto> normal_menus,
        List<EventMenuDto> event_menus) {

    public static RamenShopBasicInfoResponse from(
            RamenShop ramenShop,
            String imageUrl,
            List<NormalMenuDto> normalMenus,
            List<EventMenuDto> eventMenus,
            boolean isBookmarked
    ){
        return new RamenShopBasicInfoResponse(
                ramenShop.getId(),
                ramenShop.getName(),
                ramenShop.getBranchName(),
                ramenShop.getNaverMapId(),
                imageUrl,
                ramenShop.getAddress().fullAddress(),
                ramenShop.getInstagramUrl(),
                ramenShop.getCatchTableUrl(),
                ramenShop.getDescription(),
                BusinessHoursDto.from(ramenShop.getBusinessHours()),
                ShopStatDto.from(ramenShop.getStats()),
                isBookmarked,
                ramenShop.getTags(),
                normalMenus,
                eventMenus
        );
    }
}
