package com.raota.domain.community.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class CommunityPostSearchRequest {
    @Schema(description = "글 카테고리 필터 (예: 맛집후기)")
    private String category;
}
