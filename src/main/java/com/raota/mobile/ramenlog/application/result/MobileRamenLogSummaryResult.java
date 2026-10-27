package com.raota.mobile.ramenlog.application.result;

import com.raota.mobile.ramenlog.domain.model.LogVisibility;
import com.raota.mobile.ramenlog.domain.model.RevisitIntention;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record MobileRamenLogSummaryResult(
        String id,
        MobileRamenLogDetailResult.Author author,
        MobileRamenLogDetailResult.Shop shop,
        String menuName,
        String ramenType,
        LocalDate visitedAt,
        List<String> imageUrls,
        String note,
        List<String> tasteNoteCodes,
        RevisitIntention revisitIntention,
        LogVisibility visibility,
        MobileRamenLogDetailResult.Scores scores,
        int likeCount,
        int commentCount,
        boolean isLiked,
        boolean isMine,
        Instant createdAt) {}
