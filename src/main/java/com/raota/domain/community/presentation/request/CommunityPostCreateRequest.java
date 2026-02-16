package com.raota.domain.community.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class CommunityPostCreateRequest {
    @Schema(description = "글 카테고리")
    private String category;

    @Schema(description = "맛집후기 카테고리일 때 라멘집 ID, 그 외에는 null", nullable = true)
    private Long ramenShopId;

    @Schema(description = "글 제목")
    private String title;

    @Schema(description = "썸네일 이미지 URL(썸네일 파일을 올리지 않는 경우)", nullable = true)
    private String thumbnailUrl;

    @Schema(description = "본문 포맷", allowableValues = {"MARKDOWN", "PLAIN", "TIPTAP_JSON"})
    private String contentFormat;

    @Schema(description = "본문(마크다운/일반 텍스트/TipTap JSON 문자열). 이미지 URL은 본문에 포함")
    private String content;
}
