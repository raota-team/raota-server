package com.raota.mobile.ramenlog.infrastructure.messaging;

import com.raota.mobile.account.domain.event.MobileMemberPurgedEvent;
import com.raota.mobile.ramenlog.application.service.MobileRamenLogPurgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 회원 익명화 트랜잭션 안에서 회원 소유의 라멘 기록을 정리한다. */
@Component
@RequiredArgsConstructor
public class MobileRamenLogPurgeListener {

    private final MobileRamenLogPurgeService purges;

    @EventListener
    public void onMemberPurged(MobileMemberPurgedEvent event) {
        purges.removeForPurgedUser(event.userId());
    }

}
