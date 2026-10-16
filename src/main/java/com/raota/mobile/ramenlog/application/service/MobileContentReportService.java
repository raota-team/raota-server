package com.raota.mobile.ramenlog.application.service;

import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import com.raota.mobile.ramenlog.application.command.MobileReportCommand;
import com.raota.mobile.ramenlog.domain.model.MobileRamenLogComment;
import com.raota.mobile.ramenlog.domain.repository.MobileContentReportRepository;
import com.raota.mobile.ramenlog.domain.repository.MobileRamenLogCommentRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원이 볼 수 있는 기록·댓글만 신고를 받는다. 같은 대상을 다시 신고하면 접수된 1건을 유지하고 성공으로 응답한다. 새 신고는 WARN
 * 로그(Discord)로 운영자에게 알린다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MobileContentReportService {

    private final MobileContentReportRepository reports;

    private final MobileRamenLogCommentRepository comments;

    private final MobileRamenLogVisibility visibility;

    private final Clock clock;

    @Transactional
    public void report(Long reporterId, MobileReportCommand command) {
        Long authorId = switch (command.targetType()) {
            case RAMEN_LOG -> visibility.visible(command.targetId(), reporterId).getUserId();
            case RAMEN_LOG_COMMENT -> visibleComment(command.targetId(), reporterId).getUserId();
        };
        if (authorId.equals(reporterId)) {
            throw new MobileException(MobileErrorCode.VALIDATION_ERROR, "자기 글은 신고할 수 없습니다.");
        }
        int inserted = reports.insertIfAbsent(reporterId, command.targetType().name(), command.targetId(),
                command.reason().name(), Instant.now(clock).truncatedTo(ChronoUnit.MICROS));
        if (inserted == 1) {
            log.warn("모바일 콘텐츠 신고 접수: targetType={}, targetId={}, reason={}, reporterId={}", command.targetType(),
                    command.targetId(), command.reason(), reporterId);
        }
    }

    /** 삭제된 댓글이나 볼 수 없는 기록에 달린 댓글은 404다. */
    private MobileRamenLogComment visibleComment(Long commentId, Long reporterId) {
        MobileRamenLogComment comment = comments.findById(commentId)
            .filter(found -> found.getDeletedAt() == null)
            .orElseThrow(() -> new MobileException(MobileErrorCode.RESOURCE_NOT_FOUND, "댓글을 찾을 수 없습니다."));
        visibility.visible(comment.getRamenLogId(), reporterId);
        return comment;
    }

}
