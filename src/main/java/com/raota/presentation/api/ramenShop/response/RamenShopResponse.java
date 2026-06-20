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
        Integer viewCount,
        long ramenLogCount,
        List<String> ramenLogPreviewImageUrls
){
    public RamenShopResponse(
            Long id,
            String name,
            String tagLine,
            String region,
            List<String> tags,
            String thumbnailUrl,
            Integer visits,
            Integer viewCount
    ) {
        this(id, name, tagLine, region, tags, thumbnailUrl, visits, viewCount, 0L, List.of());
    }
}
