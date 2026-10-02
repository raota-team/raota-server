package com.raota.mobile.shop.infrastructure.messaging;

import com.raota.mobile.account.domain.event.MobileMemberPurgedEvent;
import com.raota.mobile.shop.application.port.MobileShopBookmarkPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 회원 익명화 트랜잭션 안에서 회원 소유의 북마크를 정리한다. */
@Component
@RequiredArgsConstructor
public class MobileShopPurgeListener {

    private final MobileShopBookmarkPort bookmarks;

    @EventListener
    public void onMemberPurged(MobileMemberPurgedEvent event) {
        bookmarks.removeForPurgedUser(event.userId());
    }

}
