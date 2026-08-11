package com.raota.ramenshop.application.service;

import com.raota.ramenshop.domain.model.Bookmark;
import com.raota.account.domain.member.model.MemberProfile;
import com.raota.ramenshop.domain.repository.BookmarkRepository;
import com.raota.account.domain.member.repository.MemberRepository;
import com.raota.ramenshop.domain.model.RamenShop;
import com.raota.ramenshop.domain.repository.RamenShopRepository;
import com.raota.global.cache.CacheInvalidationPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final MemberRepository memberRepository;
    private final RamenShopRepository ramenShopRepository;
    private final CacheInvalidationPublisher cacheInvalidationPublisher;

    @Transactional(readOnly = true)
    public boolean isBookmarked(Long memberId, Long shopId) {
        if (memberId == null) {
            return false;
        }

        return bookmarkRepository.existsByMemberProfileIdAndRamenShopIdAndIsDeletedFalse(memberId, shopId);
    }

    public boolean toggleBookmark(Long memberId, Long shopId) {
        boolean bookmarked = bookmarkRepository.findByMemberProfileIdAndRamenShopId(memberId, shopId)
                .map(bookmark -> {
                    boolean newDeletedStatus = !bookmark.isDeleted();
                    bookmark.changeStatus(newDeletedStatus);

                    MemberProfile member = bookmark.getMemberProfile();
                    RamenShop shop = bookmark.getRamenShop();

                    if (newDeletedStatus) {
                        member.decreaseBookmarkCount();
                        shop.decreaseBookmarkCount();
                    } else {
                        member.increaseBookmarkCount();
                        shop.increaseBookmarkCount();
                    }

                    return !newDeletedStatus; // 북마크 활성화 상태(isDeleted=false)이면 true 반환
                })
                .orElseGet(() -> {
                    MemberProfile member = memberRepository.findById(memberId)
                            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
                    RamenShop shop = ramenShopRepository.findById(shopId)
                            .orElseThrow(() -> new IllegalArgumentException("라멘 가게를 찾을 수 없습니다."));

                    Bookmark bookmark = Bookmark.builder()
                            .memberProfile(member)
                            .ramenShop(shop)
                            .isDeleted(false)
                            .build();

                    member.increaseBookmarkCount();
                    shop.increaseBookmarkCount();

                    bookmarkRepository.save(bookmark);
                    return true;
                });

        cacheInvalidationPublisher.publish("ramenShopDetail", String.valueOf(shopId));
        return bookmarked;
    }
}
