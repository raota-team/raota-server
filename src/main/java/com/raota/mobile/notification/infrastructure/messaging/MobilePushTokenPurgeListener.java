package com.raota.mobile.notification.infrastructure.messaging;

import com.raota.mobile.account.domain.event.MobileMemberPurgedEvent;
import com.raota.mobile.notification.application.service.MobilePushTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 회원 익명화 트랜잭션 안에서 회원의 기기 토큰을 지운다. */
@Component
@RequiredArgsConstructor
public class MobilePushTokenPurgeListener {

    private final MobilePushTokenService tokens;

    @EventListener
    public void onMemberPurged(MobileMemberPurgedEvent event) {
        tokens.removeForPurgedUser(event.userId());
    }
}
