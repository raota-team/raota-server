package com.raota.presentation.api.ramenShop.response;

import java.util.List;

public record RamenShopResponse(
        Long id,
        String name,
        String tagLine,
        String region,
        List<String> tags,
        String thumbnailUrl,
        Integer visits,
        Integer viewCount
){
}
