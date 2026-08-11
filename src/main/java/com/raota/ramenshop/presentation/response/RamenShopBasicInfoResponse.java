package com.raota.ramenshop.presentation.response;

import com.raota.ramenshop.presentation.response.BusinessHoursDto;
import com.raota.ramenshop.presentation.response.EventMenuDto;
import com.raota.ramenshop.presentation.response.NormalMenuDto;
import com.raota.ramenshop.presentation.response.ShopStatDto;
import com.raota.ramenshop.domain.model.RamenShop;
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
        String detailed_description,
        BusinessHoursDto business_hours,
        ShopStatDto stats,
        boolean is_bookmarked,
        List<String> tags,
        List<NormalMenuDto> normal_menus,
        List<EventMenuDto> event_menus) {

    public RamenShopBasicInfoResponse withBookmark(boolean isBookmarked) {
        return new RamenShopBasicInfoResponse(
                id,
                name,
                branch_name,
                naver_map_id,
                image_url,
                address,
                instagram_url,
                catchTableUrl,
                description,
                detailed_description,
                business_hours,
                stats,
                isBookmarked,
                tags,
                normal_menus,
                event_menus
        );
    }

    public RamenShopBasicInfoResponse withViewCount(int viewCount) {
        ShopStatDto nextStats = stats == null
                ? new ShopStatDto(viewCount, 0, 0)
                : stats.withViewCount(viewCount);

        return new RamenShopBasicInfoResponse(
                id,
                name,
                branch_name,
                naver_map_id,
                image_url,
                address,
                instagram_url,
                catchTableUrl,
                description,
                detailed_description,
                business_hours,
                nextStats,
                is_bookmarked,
                tags,
                normal_menus,
                event_menus
        );
    }

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
                descriptionForDetailResponse(ramenShop),
                ramenShop.getDetailedDescription(),
                BusinessHoursDto.from(ramenShop.getBusinessHours()),
                ShopStatDto.from(ramenShop.getStats()),
                isBookmarked,
                ramenShop.getTags(),
                normalMenus,
                eventMenus
        );
    }

    private static String descriptionForDetailResponse(RamenShop ramenShop) {
        String detailedDescription = ramenShop.getDetailedDescription();
        if (detailedDescription != null && !detailedDescription.isBlank()) {
            return detailedDescription;
        }
        return ramenShop.getDescription();
    }
}
