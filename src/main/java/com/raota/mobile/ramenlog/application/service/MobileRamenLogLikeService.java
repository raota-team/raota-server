package com.raota.mobile.ramenlog.application.service;

import com.raota.mobile.ramenlog.application.result.MobileRamenLogLikeResult;
import com.raota.mobile.ramenlog.domain.model.MobileRamenLog;
import com.raota.mobile.ramenlog.domain.repository.MobileRamenLogLikeRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 기록 행을 잠근 뒤 공감 행과 공감 수를 함께 바꿔 같은 요청이 반복되거나 겹쳐도 수가 한 번만 바뀐다. */
@Service
@RequiredArgsConstructor
public class MobileRamenLogLikeService {

    private final MobileRamenLogVisibility visibility;

    private final MobileRamenLogLikeRepository likes;

    private final Clock clock;

    @Transactional
    public MobileRamenLogLikeResult like(Long logId, Long userId) {
        MobileRamenLog ramenLog = visibility.visibleForUpdate(logId, userId);
        if (likes.insertIfAbsent(userId, logId, Instant.now(clock).truncatedTo(ChronoUnit.MICROS)) == 1) {
            ramenLog.addLike();
        }
        return new MobileRamenLogLikeResult(true, ramenLog.getLikeCount());
    }

    @Transactional
    public MobileRamenLogLikeResult unlike(Long logId, Long userId) {
        MobileRamenLog ramenLog = visibility.visibleForUpdate(logId, userId);
        if (likes.deleteLike(userId, logId) == 1) {
            ramenLog.removeLike();
        }
        return new MobileRamenLogLikeResult(false, ramenLog.getLikeCount());
    }
}
