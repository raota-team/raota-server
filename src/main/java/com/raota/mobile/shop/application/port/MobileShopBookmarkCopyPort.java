package com.raota.mobile.shop.application.port;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/** v1 북마크 테이블을 읽고 v2 북마크 테이블에 쓴다. 시각은 저장된 값 그대로 주고받는다. */
public interface MobileShopBookmarkCopyPort {

    /** 취소된 북마크를 포함한 v1 북마크 전체를 ID 순으로 읽는다. */
    List<LegacyBookmark> legacyBookmarks();

    List<BookmarkKey> bookmarkKeys();

    void insert(BookmarkKey key, LocalDateTime createdAt);

    /** 매장의 북마크 수를 v2 북마크 행 수로 다시 맞춘다. */
    void recountBookmarks(Collection<Long> shopIds);

    record LegacyBookmark(Long memberId, Long shopId, LocalDateTime markingAt, boolean deleted) {
    }

    record BookmarkKey(Long userId, Long shopId) {
    }

}
