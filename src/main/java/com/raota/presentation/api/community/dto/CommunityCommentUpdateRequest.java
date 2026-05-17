package com.raota.presentation.api.community.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class CommunityCommentUpdateRequest {
    @Schema(description = "댓글 내용")
    private String content;
}
