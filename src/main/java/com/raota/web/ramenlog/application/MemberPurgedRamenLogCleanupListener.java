package com.raota.web.ramenlog.application;

import com.raota.web.account.domain.event.MemberPurgedEvent;
import com.raota.web.ramenlog.domain.repository.RamenLogLikeRepository;
import com.raota.web.ramenlog.domain.repository.RamenLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberPurgedRamenLogCleanupListener {

    private final RamenLogLikeRepository ramenLogLikeRepository;
    private final RamenLogRepository ramenLogRepository;

    @EventListener
    public void deleteMemberRamenLogs(MemberPurgedEvent event) {
        Long memberId = event.memberId();
        ramenLogLikeRepository.deleteAllByMemberId(memberId);
        ramenLogLikeRepository.deleteAllByRamenLogAuthorId(memberId);
        ramenLogRepository.deleteAllByAuthorId(memberId);
    }
}
