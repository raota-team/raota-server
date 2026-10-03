package com.raota.mobile.ramenlog.application.result;

import com.raota.mobile.ramenlog.domain.model.LogVisibility;
import com.raota.mobile.ramenlog.domain.model.RevisitIntention;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record MobileRamenLogDetail(String id, Author author, Shop shop, String menuName, String ramenType,
        LocalDate visitedAt, List<String> imageUrls, String note, List<String> tasteNoteCodes,
        RevisitIntention revisitIntention, LogVisibility visibility, Scores scores, int likeCount, int commentCount,
        boolean isLiked, boolean isMine, Instant createdAt, Instant updatedAt, List<Object> commentsPreview,
        String commentsNextCursor) {

    public record Author(String id, String nickname, String avatarUrl, int logCount) {
    }

    public record Shop(String id, String name, String branchName, String region) {
    }

    public record Scores(int satisfaction, int brothDensity, int noodleFirmness, int topping, int revisit) {
    }

}
