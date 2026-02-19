package com.raota.domain.community.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class CommunityRamenShopSearchRequest {
    @Schema(description = "가게명 검색 키워드")
    private String keyword;
}
