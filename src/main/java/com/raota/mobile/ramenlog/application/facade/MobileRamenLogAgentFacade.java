package com.raota.mobile.ramenlog.application.facade;

import com.raota.mobile.ramenlog.domain.repository.MobileRamenLogRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** AI 추천 성과 집계(agent)가 쓰는 라멘 기록 조회 API다. */
@Service
@RequiredArgsConstructor
public class MobileRamenLogAgentFacade {

    private final MobileRamenLogRepository logs;

    /** 이 회원들이 {@code [from, to)} 사이에 쓰고 지우지 않은 기록의 회원·매장·작성 시각이다. */
    @Transactional(readOnly = true)
    public List<MobileRamenLogActivity> logsCreatedBetween(Collection<Long> userIds, Instant from, Instant to) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return logs.findLiveCreatedBetween(userIds, from, to).stream()
                .map(log -> new MobileRamenLogActivity(log.getUserId(), log.getShopId(), log.getCreatedAt()))
                .toList();
    }

    /** 회원이 매장에 쓴 기록과 그 작성 시각이다. */
    public record MobileRamenLogActivity(Long userId, Long shopId, Instant createdAt) {}
}
