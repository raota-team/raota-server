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
        if (bookmarkRepository.existsByMemberProfileIdAndRamenShopId(memberId, shopId)) {
            bookmarkRepository.deleteByMemberProfileIdAndRamenShopId(memberId, shopId);
            return false; // 북마크 해제됨
        }

        MemberProfile member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        RamenShop shop = ramenShopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("라멘 가게를 찾을 수 없습니다."));

        Bookmark bookmark = Bookmark.builder()
                .memberProfile(member)
                .ramenShop(shop)
                .build();

        bookmarkRepository.save(bookmark);
        return true; // 북마크 등록됨
    }
}
