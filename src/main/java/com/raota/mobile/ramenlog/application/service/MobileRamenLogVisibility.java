package com.raota.mobile.ramenlog.application.service;

import com.raota.mobile.account.application.facade.MobileAccountRamenLogFacade;
import com.raota.mobile.account.application.facade.MobileAccountRamenLogFacade.MobileAuthor;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import com.raota.mobile.ramenlog.domain.model.MobileRamenLog;
import com.raota.mobile.ramenlog.domain.repository.MobileRamenLogRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 공감·댓글 대상 기록을 상세 조회와 같은 기준으로 고른다. 삭제됐거나, 타인의 비공개 기록이거나, 작성자가 활성 회원이 아니면 존재를 드러내지 않고 404로
 * 응답한다.
 */
@Component
@RequiredArgsConstructor
class MobileRamenLogVisibility {

    private final MobileRamenLogRepository logs;

    private final MobileAccountRamenLogFacade accounts;

    MobileRamenLog visible(Long logId, Long viewerId) {
        return check(logs.findById(logId), viewerId);
    }

    /** 공감·댓글 수를 바꾸기 전에 기록 행을 잠근다. */
    MobileRamenLog visibleForUpdate(Long logId, Long viewerId) {
        return check(logs.findByIdForUpdate(logId), viewerId);
    }

    private MobileRamenLog check(Optional<MobileRamenLog> found, Long viewerId) {
        MobileRamenLog ramenLog = found.orElseThrow(MobileRamenLogVisibility::notFound);
        if (ramenLog.getDeletedAt() != null || !ramenLog.visibleTo(viewerId)) {
            throw notFound();
        }
        MobileAuthor author = accounts.authors(List.of(ramenLog.getUserId())).get(ramenLog.getUserId());
        if (author == null || !author.active()) {
            throw notFound();
        }
        return ramenLog;
    }

    private static MobileException notFound() {
        return new MobileException(MobileErrorCode.RESOURCE_NOT_FOUND, "기록을 찾을 수 없습니다.");
    }
}
