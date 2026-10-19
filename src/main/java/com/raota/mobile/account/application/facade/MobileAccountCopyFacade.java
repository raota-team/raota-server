package com.raota.mobile.account.application.facade;

import com.raota.mobile.account.application.service.MobileMemberCopyService;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** v1 → v2 복사(#59)에서 기록·북마크 단계가 쓰는 회원 정보다. */
@Service
@RequiredArgsConstructor
public class MobileAccountCopyFacade {

    private final MobileMemberCopyService members;

    private final MobileUserRepository users;

    /** v1 회원에서 복사된 v2 회원 ID다. v1 회원 ID와 같다. */
    public Set<Long> copiedMemberIds() {
        return members.copiedMemberIds();
    }

    /** 회원별 기록 수를 실제 기록 수로 다시 맞춘다. */
    @Transactional
    public void updateLogCounts(Map<Long, Integer> logCounts) {
        logCounts.forEach(users::updateLogCount);
    }

}
