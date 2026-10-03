package com.raota.mobile.ramenlog.application.service;

import com.raota.mobile.ramenlog.domain.repository.MobileRamenLogImageRepository;
import com.raota.mobile.ramenlog.domain.repository.MobileRamenLogRepository;
import com.raota.mobile.ramenlog.domain.repository.MobileRamenLogRepository.ShopLogTotals;
import com.raota.mobile.shop.application.facade.MobileShopRamenLogFacade;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 회원 익명화 트랜잭션에서 그 회원의 기록·사진 행을 지우고 활성 기록의 매장 집계를 매장별로 한 번씩 되돌린다. */
@Service
@RequiredArgsConstructor
public class MobileRamenLogPurgeService {

    private final MobileRamenLogRepository logs;

    private final MobileRamenLogImageRepository images;

    private final MobileShopRamenLogFacade shops;

    @Transactional
    public void removeForPurgedUser(Long userId) {
        List<ShopLogTotals> totals = logs.sumLiveLogsByShop(userId);
        images.deleteForUser(userId);
        logs.deleteForUser(userId);
        for (ShopLogTotals total : totals) {
            shops.recordLogsRemoved(total.getShopId(), Math.toIntExact(total.getLogs()),
                    Math.toIntExact(total.getScoreSum()), Math.toIntExact(total.getScoredLogs()));
        }
    }

}
