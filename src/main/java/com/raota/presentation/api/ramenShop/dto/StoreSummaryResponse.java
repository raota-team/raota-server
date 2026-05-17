package com.raota.presentation.api.ramenShop.dto;

import java.util.List;

public record StoreSummaryResponse(
        Long id,
        String name,
        String tagLine,
        String region,
        List<String> tags,
        String thumbnailUrl,
        Integer visits
        ){
}
