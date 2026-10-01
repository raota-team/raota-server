package com.raota.mobile.shop.application.service;

import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import com.raota.mobile.shop.application.port.MobileShopBookmarkPort;
import com.raota.mobile.shop.domain.repository.MobileShopRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 북마크의 실제 추가·삭제와 매장 카운터 갱신을 한 트랜잭션에 묶는다. */
@Service
@RequiredArgsConstructor
public class MobileShopBookmarkService {

    private final MobileShopBookmarkPort bookmarks;

    private final MobileShopRepository shops;

    private final Clock clock;

    @Transactional
    public void add(Long userId, Long shopId) {
        if (shops.findByIdAndPublishedTrueAndDeletedAtIsNull(shopId).isEmpty()) {
            throw new MobileException(MobileErrorCode.RESOURCE_NOT_FOUND, "매장을 찾을 수 없습니다.");
        }
        bookmarks.add(userId, shopId, Instant.now(clock).truncatedTo(ChronoUnit.MICROS));
    }

    @Transactional
    public void remove(Long userId, Long shopId) {
        bookmarks.remove(userId, shopId);
    }

}
