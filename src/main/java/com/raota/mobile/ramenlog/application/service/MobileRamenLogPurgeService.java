package com.raota.mobile.ramenlog.application.service;

import com.raota.mobile.ramenlog.domain.repository.MobileContentReportRepository;
import com.raota.mobile.ramenlog.domain.repository.MobileRamenLogCommentRepository;
import com.raota.mobile.ramenlog.domain.repository.MobileRamenLogCommentRepository.LogCommentTotals;
import com.raota.mobile.ramenlog.domain.repository.MobileRamenLogImageRepository;
import com.raota.mobile.ramenlog.domain.repository.MobileRamenLogLikeRepository;
import com.raota.mobile.ramenlog.domain.repository.MobileRamenLogRepository;
import com.raota.mobile.ramenlog.domain.repository.MobileRamenLogRepository.ShopLogTotals;
import com.raota.mobile.ramenlog.domain.repository.MobileUserBlockRepository;
import com.raota.mobile.shop.application.facade.MobileShopRamenLogFacade;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 익명화 트랜잭션에서 그 회원의 신고·차단·공감·댓글·기록·사진 행과, 그 회원 기록·댓글에 달린 공감·댓글·신고를 지운다. 그 회원이 다른 기록에 남긴
 * 공감·댓글 수와 활성 기록의 매장 집계는 기록·매장마다 한 번씩 되돌린다.
 */
@Service
@RequiredArgsConstructor
public class MobileRamenLogPurgeService {

    private final MobileRamenLogRepository logs;

    private final MobileRamenLogImageRepository images;

    private final MobileRamenLogLikeRepository likes;

    private final MobileRamenLogCommentRepository comments;

    private final MobileContentReportRepository reports;

    private final MobileUserBlockRepository blocks;

    private final MobileShopRamenLogFacade shops;

    @Transactional
    public void removeForPurgedUser(Long userId) {
        reports.deleteByReporter(userId);
        reports.deleteTargetingContentOf(userId);
        blocks.deleteInvolving(userId);
        logs.decrementLikesBy(userId);
        likes.deleteByLiker(userId);
        likes.deleteOnLogsOf(userId);
        for (LogCommentTotals total : comments.countLiveByLog(userId)) {
            logs.removeComments(total.getLogId(), Math.toIntExact(total.getComments()));
        }
        comments.deleteByWriter(userId);
        comments.deleteOnLogsOf(userId);
        List<ShopLogTotals> totals = logs.sumLiveLogsByShop(userId);
        images.deleteForUser(userId);
        logs.deleteForUser(userId);
        for (ShopLogTotals total : totals) {
            shops.recordLogsRemoved(total.getShopId(), Math.toIntExact(total.getLogs()),
                    Math.toIntExact(total.getScoreSum()), Math.toIntExact(total.getScoredLogs()));
        }
    }

}
