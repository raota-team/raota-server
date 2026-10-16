package com.raota.mobile.ramenlog.application.service;

import com.raota.mobile.account.application.facade.MobileAccountRamenLogFacade;
import com.raota.mobile.account.application.facade.MobileAccountRamenLogFacade.MobileAuthor;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import com.raota.mobile.ramenlog.application.result.MobileRamenLogLikeResult;
import com.raota.mobile.ramenlog.domain.model.MobileRamenLog;
import com.raota.mobile.ramenlog.domain.repository.MobileRamenLogLikeRepository;
import com.raota.mobile.ramenlog.domain.repository.MobileRamenLogRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 기록 행을 잠근 뒤 공감 행과 공감 수를 함께 바꿔 같은 요청이 반복되거나 겹쳐도 수가 한 번만 바뀐다. */
@Service
@RequiredArgsConstructor
public class MobileRamenLogLikeService {

    private final MobileRamenLogRepository logs;

    private final MobileRamenLogLikeRepository likes;

    private final MobileAccountRamenLogFacade accounts;

    private final Clock clock;

    @Transactional
    public MobileRamenLogLikeResult like(Long logId, Long userId) {
        MobileRamenLog log = visibleForUpdate(logId, userId);
        if (likes.insertIfAbsent(userId, logId, Instant.now(clock).truncatedTo(ChronoUnit.MICROS)) == 1) {
            log.addLike();
        }
        return new MobileRamenLogLikeResult(true, log.getLikeCount());
    }

    @Transactional
    public MobileRamenLogLikeResult unlike(Long logId, Long userId) {
        MobileRamenLog log = visibleForUpdate(logId, userId);
        if (likes.deleteLike(userId, logId) == 1) {
            log.removeLike();
        }
        return new MobileRamenLogLikeResult(false, log.getLikeCount());
    }

    /** 상세 조회와 같은 기준으로, 볼 수 없는 기록은 존재를 드러내지 않고 404로 응답한다. */
    private MobileRamenLog visibleForUpdate(Long logId, Long userId) {
        MobileRamenLog log = logs.findByIdForUpdate(logId).orElseThrow(this::notFound);
        if (log.getDeletedAt() != null || !log.visibleTo(userId)) {
            throw notFound();
        }
        MobileAuthor author = accounts.authors(List.of(log.getUserId())).get(log.getUserId());
        if (author == null || !author.active()) {
            throw notFound();
        }
        return log;
    }

    private MobileException notFound() {
        return new MobileException(MobileErrorCode.RESOURCE_NOT_FOUND, "기록을 찾을 수 없습니다.");
    }

}
