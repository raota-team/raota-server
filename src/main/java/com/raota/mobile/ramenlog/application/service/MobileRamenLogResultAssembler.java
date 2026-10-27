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

    /** 기록 상세 화면용이다. 댓글 첫 페이지를 함께 담는다. */
    static MobileRamenLogDetailResult toDetail(
            MobileRamenLog log,
            MobileAuthor author,
            MobileShopRef shop,
            List<String> imageUrls,
            boolean liked,
            CursorPage<MobileRamenLogCommentResult> comments,
            Long viewerId) {
        return new MobileRamenLogDetailResult(
                log.getId().toString(),
                toAuthor(author),
                toShop(shop),
                log.getMenuName(),
                log.getRamenType(),
                log.getVisitedAt(),
                imageUrls,
                log.getNote(),
                log.getTasteNoteCodes(),
                log.getRevisitIntention(),
                log.getVisibility(),
                toScores(log),
                log.getLikeCount(),
                log.getCommentCount(),
                liked,
                log.getUserId().equals(viewerId),
                log.getCreatedAt(),
                log.getUpdatedAt(),
                comments.items(),
                comments.nextCursor());
    }

    /** 피드·내 기록 목록의 카드용이다. */
    static MobileRamenLogSummaryResult toSummary(
            MobileRamenLog log,
            MobileAuthor author,
            MobileShopRef shop,
            List<String> imageUrls,
            boolean liked,
            Long viewerId) {
        return new MobileRamenLogSummaryResult(
                log.getId().toString(),
                toAuthor(author),
                toShop(shop),
                log.getMenuName(),
                log.getRamenType(),
                log.getVisitedAt(),
                imageUrls,
                log.getNote(),
                log.getTasteNoteCodes(),
                log.getRevisitIntention(),
                log.getVisibility(),
                toScores(log),
                log.getLikeCount(),
                log.getCommentCount(),
                liked,
                log.getUserId().equals(viewerId),
                log.getCreatedAt());
    }

    /** 취향 결산에 쓰는 가벼운 기록 항목이다(작성자·사진·본문 없음). */
    static MobileRamenLogSummaryItemResult toSummaryItem(MobileRamenLog log, MobileShopRef shop) {
        return new MobileRamenLogSummaryItemResult(
                log.getId().toString(),
                log.getVisitedAt(),
                new MobileRamenLogSummaryItemResult.Shop(shop.id().toString(), shop.name(), shop.branchName()),
                log.getMenuName(),
                log.getRamenType(),
                toScores(log));
    }

    static MobileRamenLogCommentResult toComment(MobileRamenLogComment comment, MobileAuthor author, Long viewerId) {
        return new MobileRamenLogCommentResult(
                comment.getId().toString(),
                comment.getRamenLogId().toString(),
                toAuthor(author),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUserId().equals(viewerId));
    }

    private static MobileRamenLogDetailResult.Author toAuthor(MobileAuthor author) {
        return new MobileRamenLogDetailResult.Author(
                author.id().toString(), author.nickname(), author.avatarUrl(), author.logCount());
    }

    private static MobileRamenLogDetailResult.Shop toShop(MobileShopRef shop) {
        return new MobileRamenLogDetailResult.Shop(shop.id().toString(), shop.name(), shop.branchName(), shop.region());
    }

    /** 점수가 없는 기록(v1에서 옮긴 기록)은 {@code null}이다. */
    private static MobileRamenLogDetailResult.Scores toScores(MobileRamenLog log) {
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
