package com.raota.domain.community.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class CommunityCommentCreateRequest {
    @Schema(description = "댓글 내용")
    private String content;

    @Schema(description = "부모 댓글 ID(답글일 때만)", nullable = true)
    private Long parentCommentId;
}
