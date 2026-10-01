package com.raota.mobile.shop.application.port;

import com.raota.mobile.common.cursor.Cursor;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/** 회원별 북마크 변경과 생성시각 기준 커서 조회를 제공한다. */
public interface MobileShopBookmarkPort {

    void add(Long userId, Long shopId, Instant createdAt);

    void remove(Long userId, Long shopId);

    void removeForPurgedUser(Long userId);

    Set<Long> bookmarkedShopIds(Long userId, List<Long> shopIds);

    List<SavedShop> saved(Long userId, Cursor cursor, int limit);

    record SavedShop(Long shopId, Cursor position) {
    }

}
