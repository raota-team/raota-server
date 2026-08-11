package com.raota.ramenshop.presentation.response;

import com.raota.ramenshop.domain.model.ShopStats;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ShopStatDto {
    @Schema(example = "3021")
    private int view_count;
    @Schema(example = "1250")
    private int visit_count;
    @Schema(example = "342")
    private int bookmark_count;

    public static ShopStatDto from(ShopStats stats) {
        if (stats == null) {
            return new ShopStatDto(0, 0, 0);
        }
        return new ShopStatDto(
                stats.viewCount(),
                stats.visitCount(),
                stats.bookmarkCount()
        );
    }

    public ShopStatDto withViewCount(int viewCount) {
        return new ShopStatDto(viewCount, visit_count, bookmark_count);
    }
}
