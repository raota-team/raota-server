package com.raota.mobile.shop.application.port;

import com.raota.mobile.common.cursor.Cursor;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/** 회원별 북마크 변경과 생성시각 기준 커서 조회를 제공한다. */
public interface MobileShopBookmarkPort {

    void add(Long userId, Long shopId, Instant createdAt);

    void remove(Long userId, Long shopId);

    void removeForPurgedUser(Long userId);

    Set<Long> bookmarkedShopIds(Long userId, List<Long> shopIds);

    List<SavedShop> saved(Long userId, Cursor cursor, int limit);

    /** 이 회원들이 {@code [from, to)} 사이에 만든 북마크다. 해제한 북마크는 행이 없어 빠진다. */
    List<CreatedBookmark> createdBetween(Collection<Long> userIds, Instant from, Instant to);

    record SavedShop(Long shopId, Cursor position) {
    }

    record CreatedBookmark(Long userId, Long shopId, Instant createdAt) {
    }

}
