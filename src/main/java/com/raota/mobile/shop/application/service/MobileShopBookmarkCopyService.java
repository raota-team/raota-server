package com.raota.mobile.shop.application.service;

import com.raota.mobile.account.application.facade.MobileAccountCopyFacade;
import com.raota.mobile.common.copy.LegacyCopyExclusion;
import com.raota.mobile.common.copy.LegacyCopyReport;
import com.raota.mobile.common.copy.LegacyCopyTally;
import com.raota.mobile.common.copy.LegacyKstTime;
import com.raota.mobile.shop.application.port.MobileShopBookmarkCopyPort;
import com.raota.mobile.shop.application.port.MobileShopBookmarkCopyPort.BookmarkKey;
import com.raota.mobile.shop.application.port.MobileShopBookmarkCopyPort.LegacyBookmark;
import com.raota.mobile.shop.domain.repository.MobileShopRepository;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * v1 북마크를 v2로 복사하고 옮긴 매장의 북마크 수를 다시 맞춘다(#59). 회원 복사 뒤에 실행하며, 이미 있는 (회원, 매장)은 건너뛴다.
 */
@Service
@RequiredArgsConstructor
public class MobileShopBookmarkCopyService {

    private final MobileShopBookmarkCopyPort port;

    private final MobileAccountCopyFacade accounts;

    private final MobileShopRepository shops;

    @Transactional
    public LegacyCopyReport copy() {
        Set<Long> members = accounts.copiedMemberIds();
        Set<Long> shopIds = new HashSet<>(shops.findAllIds());
        Set<BookmarkKey> existing = new HashSet<>(port.bookmarkKeys());
        Set<BookmarkKey> seen = new HashSet<>();
        Set<Long> touchedShops = new HashSet<>();
        LegacyCopyTally tally = new LegacyCopyTally("tb_v2_shop_bookmark");
        for (LegacyBookmark bookmark : port.legacyBookmarks()) {
            BookmarkKey key = new BookmarkKey(bookmark.memberId(), bookmark.shopId());
            // 취소·중복 여부는 v1 원본만으로 정해 몇 번을 다시 실행해도 같은 행이 같은 분류가 된다.
            if (bookmark.deleted()) {
                tally.exclude(LegacyCopyExclusion.CANCELED);
            }
            else if (!seen.add(key)) {
                tally.exclude(LegacyCopyExclusion.DUPLICATE_BOOKMARK);
            }
            else if (existing.contains(key)) {
                tally.alreadyCopied();
            }
            else if (!members.contains(bookmark.memberId())) {
                tally.exclude(LegacyCopyExclusion.MEMBER_NOT_COPIED);
            }
            else if (!shopIds.contains(bookmark.shopId())) {
                tally.exclude(LegacyCopyExclusion.SHOP_NOT_COPIED);
            }
            else {
                port.insert(key, LegacyKstTime.toUtc(bookmark.markingAt()));
                touchedShops.add(bookmark.shopId());
                tally.copied();
            }
        }
        port.recountBookmarks(touchedShops);
        return LegacyCopyReport.of(tally);
    }

}
