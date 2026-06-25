package com.raota.presentation.api.community.request;

import com.raota.application.community.query.PostSearchQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommunityPostSearchRequest {
    @Schema(
            description = "글 카테고리 필터. REVIEW, TIP, QUESTION, FREE 또는 좋아요 3개 이상인 인기글을 뜻하는 POPULAR",
            example = "POPULAR"
    )
    private String category;

    @Schema(description = "맛집후기 카테고리에서 필터링할 라멘집 ID", nullable = true)
    private Long ramenShopId;

    public PostSearchQuery toQuery() {
        return new PostSearchQuery(category, ramenShopId);
    }
}
