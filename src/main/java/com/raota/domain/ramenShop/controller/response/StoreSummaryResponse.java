package com.raota.domain.ramenShop.controller.response;

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
