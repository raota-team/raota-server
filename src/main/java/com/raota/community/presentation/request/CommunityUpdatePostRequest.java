package com.raota.community.presentation.request;

import com.raota.community.application.command.UpdatePostCommand;
import io.swagger.v3.oas.annotations.media.Schema;

public record CommunityUpdatePostRequest(
        @Schema(description = "글 카테고리")
        String category,
        @Schema(description = "맛집후기 카테고리일 때 라멘집 ID, 그 외에는 null", nullable = true)
        Long ramenShopId,
        @Schema(description = "글 제목")
        String title,
        @Schema(description = "썸네일 이미지 URL(썸네일 파일을 올리지 않는 경우)", nullable = true)
        String thumbnailUrl,
        @Schema(description = "본문 포맷", allowableValues = {"MARKDOWN", "PLAIN", "TIPTAP_JSON"})
        String contentFormat,
        @Schema(description = "본문(마크다운/일반 텍스트/TipTap JSON 문자열). 이미지 URL은 본문에 포함")
        String content
) {
        public UpdatePostCommand toCommand(Long postId, Long authorId) {
                return new UpdatePostCommand(
                        postId,
                        category,
                        ramenShopId,
                        title,
                        thumbnailUrl,
                        contentFormat,
                        content,
                        authorId
                );
        }
}
