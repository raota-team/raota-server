package com.raota.presentation.api.community.response;

import com.raota.application.community.result.HomePostResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record CommunityHomePostResponse(
        @Schema(description = "글 ID")
        Long id,
        @Schema(description = "글 제목")
        String title,
        @Schema(description = "본문 요약")
        String contentSnippet,
        @Schema(description = "작성자 정보")
        AuthorSummary author,
        @Schema(description = "댓글 수")
        long commentCount,
        @Schema(description = "조회 수")
        Integer viewCount,
        @Schema(description = "작성 일시")
        LocalDateTime createdAt
) {
    public static CommunityHomePostResponse from(HomePostResult result) {
        return new CommunityHomePostResponse(
                result.id(),
                result.title(),
                result.contentSnippet(),
                new AuthorSummary(result.author().nickname(), result.author().profileImageUrl()),
                result.commentCount(),
                result.viewCount(),
                result.createdAt()
        );
    }

    public record AuthorSummary(
            @Schema(description = "닉네임")
            String nickname,
            @Schema(description = "프로필 이미지 URL")
            String profileImageUrl
    ) {}
}
