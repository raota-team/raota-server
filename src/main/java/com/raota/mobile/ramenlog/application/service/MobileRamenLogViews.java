package com.raota.mobile.ramenlog.application.service;

import com.raota.mobile.account.application.facade.MobileAccountRamenLogFacade.MobileAuthor;
import com.raota.mobile.ramenlog.application.result.MobileRamenLogDetail;
import com.raota.mobile.ramenlog.application.result.MobileRamenLogSummary;
import com.raota.mobile.ramenlog.application.result.MobileRamenLogSummaryItem;
import com.raota.mobile.ramenlog.domain.model.MobileRamenLog;
import com.raota.mobile.shop.application.facade.MobileShopRamenLogFacade.MobileShopRef;
import java.util.List;

/** 저장된 기록의 필드를 앱의 공개 응답 형태로 변환한다. */
final class MobileRamenLogViews {

    private MobileRamenLogViews() {
    }

    static MobileRamenLogDetail detail(MobileRamenLog log, MobileAuthor author, MobileShopRef shop,
            List<String> imageUrls, Long viewerId) {
        return new MobileRamenLogDetail(log.getId().toString(), author(author), shop(shop), log.getMenuName(),
                log.getRamenType(), log.getVisitedAt(), imageUrls, log.getNote(), log.getTasteNoteCodes(),
                log.getRevisitIntention(), log.getVisibility(), scores(log), log.getLikeCount(), log.getCommentCount(),
                false, log.getUserId().equals(viewerId), log.getCreatedAt(), log.getUpdatedAt(), List.of(), null);
    }

    static MobileRamenLogSummary summary(MobileRamenLog log, MobileAuthor author, MobileShopRef shop,
            List<String> imageUrls) {
        return new MobileRamenLogSummary(log.getId().toString(), author(author), shop(shop), log.getMenuName(),
                log.getRamenType(), log.getVisitedAt(), imageUrls, log.getNote(), log.getTasteNoteCodes(),
                log.getRevisitIntention(), log.getVisibility(), scores(log), log.getLikeCount(), log.getCommentCount(),
                false, true, log.getCreatedAt());
    }

    static MobileRamenLogSummaryItem summaryItem(MobileRamenLog log, MobileShopRef shop) {
        return new MobileRamenLogSummaryItem(log.getId().toString(), log.getVisitedAt(),
                new MobileRamenLogSummaryItem.Shop(shop.id().toString(), shop.name(), shop.branchName()),
                log.getMenuName(), log.getRamenType(), scores(log));
    }

    private static MobileRamenLogDetail.Author author(MobileAuthor author) {
        return new MobileRamenLogDetail.Author(author.id().toString(), author.nickname(), author.avatarUrl(),
                author.logCount());
    }

    private static MobileRamenLogDetail.Shop shop(MobileShopRef shop) {
        return new MobileRamenLogDetail.Shop(shop.id().toString(), shop.name(), shop.branchName(), shop.region());
    }

    private static MobileRamenLogDetail.Scores scores(MobileRamenLog log) {
        if (log.getSatisfactionScore() == null) {
            return null;
        }
        return new MobileRamenLogDetail.Scores(log.getSatisfactionScore(), log.getBrothDensityScore(),
                log.getNoodleFirmnessScore(), log.getToppingScore(), log.getRevisitIntention().score());
    }

}
