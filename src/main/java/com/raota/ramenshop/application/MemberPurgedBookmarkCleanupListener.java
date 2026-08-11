package com.raota.ramenshop.application;

import com.raota.account.domain.event.MemberPurgedEvent;
import com.raota.ramenshop.domain.repository.BookmarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberPurgedBookmarkCleanupListener {

    private final BookmarkRepository bookmarkRepository;

    @EventListener
    public void deleteMemberBookmarks(MemberPurgedEvent event) {
        bookmarkRepository.deleteAllByMemberProfileId(event.memberId());
    }
}
