package com.raota.domain.member.service;

import com.raota.domain.member.model.Bookmark;
import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.BookmarkRepository;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
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

    public boolean toggleBookmark(Long memberId, Long shopId) {
        return bookmarkRepository.findByMemberProfileIdAndRamenShopId(memberId, shopId)
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
    }
}
