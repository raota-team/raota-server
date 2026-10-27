package com.raota.mobile.ramenlog.application.service;

import com.raota.mobile.account.application.facade.MobileAccountRamenLogFacade.MobileAuthor;
import com.raota.mobile.common.cursor.CursorPage;
import com.raota.mobile.ramenlog.application.result.MobileRamenLogCommentResult;
import com.raota.mobile.ramenlog.application.result.MobileRamenLogDetailResult;
import com.raota.mobile.ramenlog.application.result.MobileRamenLogSummaryItemResult;
import com.raota.mobile.ramenlog.application.result.MobileRamenLogSummaryResult;
import com.raota.mobile.ramenlog.domain.model.MobileRamenLog;
import com.raota.mobile.ramenlog.domain.model.MobileRamenLogComment;
import com.raota.mobile.shop.application.facade.MobileShopRamenLogFacade.MobileShopRef;
import java.util.List;

/** 저장된 기록·댓글을 앱 응답용 결과(상세·요약·요약 항목·댓글)로 조립한다. 서비스 안에서만 쓰는 정적 조립기다. */
final class MobileRamenLogResultAssembler {

    private MobileRamenLogResultAssembler() {}

    static MobileRamenLogDetailResult detail(
            MobileRamenLog log,
            MobileAuthor author,
            MobileShopRef shop,
            List<String> imageUrls,
            boolean liked,
            CursorPage<MobileRamenLogCommentResult> comments,
            Long viewerId) {
        return new MobileRamenLogDetailResult(
                log.getId().toString(),
                author(author),
                shop(shop),
                log.getMenuName(),
                log.getRamenType(),
                log.getVisitedAt(),
                imageUrls,
                log.getNote(),
                log.getTasteNoteCodes(),
                log.getRevisitIntention(),
                log.getVisibility(),
                scores(log),
                log.getLikeCount(),
                log.getCommentCount(),
                liked,
                log.getUserId().equals(viewerId),
                log.getCreatedAt(),
                log.getUpdatedAt(),
                comments.items(),
                comments.nextCursor());
    }

    static MobileRamenLogSummaryResult summary(
            MobileRamenLog log,
            MobileAuthor author,
            MobileShopRef shop,
            List<String> imageUrls,
            boolean liked,
            Long viewerId) {
        return new MobileRamenLogSummaryResult(
                log.getId().toString(),
                author(author),
                shop(shop),
                log.getMenuName(),
                log.getRamenType(),
                log.getVisitedAt(),
                imageUrls,
                log.getNote(),
                log.getTasteNoteCodes(),
                log.getRevisitIntention(),
                log.getVisibility(),
                scores(log),
                log.getLikeCount(),
                log.getCommentCount(),
                liked,
                log.getUserId().equals(viewerId),
                log.getCreatedAt());
    }

    static MobileRamenLogSummaryItemResult summaryItem(MobileRamenLog log, MobileShopRef shop) {
        return new MobileRamenLogSummaryItemResult(
                log.getId().toString(),
                log.getVisitedAt(),
                new MobileRamenLogSummaryItemResult.Shop(shop.id().toString(), shop.name(), shop.branchName()),
                log.getMenuName(),
                log.getRamenType(),
                scores(log));
    }

    static MobileRamenLogCommentResult comment(MobileRamenLogComment comment, MobileAuthor author, Long viewerId) {
        return new MobileRamenLogCommentResult(
                comment.getId().toString(),
                comment.getRamenLogId().toString(),
                author(author),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUserId().equals(viewerId));
    }

    private static MobileRamenLogDetailResult.Author author(MobileAuthor author) {
        return new MobileRamenLogDetailResult.Author(
                author.id().toString(), author.nickname(), author.avatarUrl(), author.logCount());
    }

    private static MobileRamenLogDetailResult.Shop shop(MobileShopRef shop) {
        return new MobileRamenLogDetailResult.Shop(shop.id().toString(), shop.name(), shop.branchName(), shop.region());
    }

    private static MobileRamenLogDetailResult.Scores scores(MobileRamenLog log) {
        if (log.getSatisfactionScore() == null) {
            return null;
        }
        return new MobileRamenLogDetailResult.Scores(
                log.getSatisfactionScore(),
                log.getBrothDensityScore(),
                log.getNoodleFirmnessScore(),
                log.getToppingScore(),
                log.getRevisitIntention().score());
    }
}
