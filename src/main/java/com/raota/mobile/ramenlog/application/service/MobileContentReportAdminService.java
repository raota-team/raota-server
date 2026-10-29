package com.raota.mobile.ramenlog.application.service;

import com.raota.mobile.account.application.facade.MobileAccountRamenLogFacade;
import com.raota.mobile.account.application.facade.MobileAccountRamenLogFacade.MobileAuthor;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import com.raota.mobile.common.page.OffsetPage;
import com.raota.mobile.ramenlog.application.command.MobileReportResolution;
import com.raota.mobile.ramenlog.application.result.MobileAdminReportResolutionResult;
import com.raota.mobile.ramenlog.application.result.MobileAdminReportResult;
import com.raota.mobile.ramenlog.application.result.MobileAdminReportResult.Person;
import com.raota.mobile.ramenlog.application.result.MobileAdminReportResult.Target;
import com.raota.mobile.ramenlog.domain.model.MobileContentReport;
import com.raota.mobile.ramenlog.domain.model.MobileRamenLog;
import com.raota.mobile.ramenlog.domain.model.MobileRamenLogComment;
import com.raota.mobile.ramenlog.domain.model.MobileRamenLogImage;
import com.raota.mobile.ramenlog.domain.model.ReportStatus;
import com.raota.mobile.ramenlog.domain.model.ReportTargetType;
import com.raota.mobile.ramenlog.domain.repository.MobileContentReportRepository;
import com.raota.mobile.ramenlog.domain.repository.MobileRamenLogCommentRepository;
import com.raota.mobile.ramenlog.domain.repository.MobileRamenLogImageRepository;
import com.raota.mobile.ramenlog.domain.repository.MobileRamenLogRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 웹의 라운지 신고 처리다. 신고 한 건을 처리하면 같은 대상에 열린 다른 신고도 같은 결과로 닫는다.
 */
@Service
@RequiredArgsConstructor
public class MobileContentReportAdminService {

    private final MobileContentReportRepository contentReportRepository;

    private final MobileRamenLogRepository ramenLogRepository;

    private final MobileRamenLogCommentRepository commentRepository;

    private final MobileRamenLogImageRepository imageRepository;

    private final MobileRamenLogService ramenLogService;

    private final MobileRamenLogCommentService commentService;

    private final MobileAccountRamenLogFacade accountFacade;

    private final Clock clock;

    /** 열린 신고는 오래된 것부터(24시간 안 처리), 닫힌 신고는 최근 처리한 것부터. */
    @Transactional(readOnly = true)
    public OffsetPage<MobileAdminReportResult> listReports(ReportStatus status, int page, int size) {
        Sort sort = Sort.by(Sort.Order.desc("resolvedAt"), Sort.Order.desc("id"));
        if (status == ReportStatus.OPEN) {
            sort = Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));
        }
        Page<MobileContentReport> found =
                contentReportRepository.findByStatus(status, PageRequest.of(page, size, sort));
        Previews previews = findPreviews(found.getContent());
        return OffsetPage.of(found, report -> toReportResult(report, previews));
    }

    @Transactional
    public MobileAdminReportResolutionResult resolveReport(Long reportId, MobileReportResolution resolution) {
        MobileContentReport report = contentReportRepository
                .findById(reportId)
                .orElseThrow(() -> new MobileException(MobileErrorCode.RESOURCE_NOT_FOUND, "신고를 찾을 수 없습니다."));
        if (report.getStatus() != ReportStatus.OPEN) {
            throw alreadyClosed();
        }
        ReportStatus closedAs = ReportStatus.DISMISSED;
        if (resolution == MobileReportResolution.DELETE_CONTENT) {
            closedAs = ReportStatus.RESOLVED;
            removeTarget(report);
        }
        int closed = contentReportRepository.closeOpen(
                report.getTargetType(),
                report.getTargetId(),
                closedAs,
                Instant.now(clock).truncatedTo(ChronoUnit.MICROS));
        // 읽은 뒤 다른 관리자가 먼저 닫았으면 롤백해서 그 결과를 덮어쓰지 않는다.
        if (closed == 0) {
            throw alreadyClosed();
        }
        return new MobileAdminReportResolutionResult(closed);
    }

    private void removeTarget(MobileContentReport report) {
        Consumer<Long> remove = switch (report.getTargetType()) {
            case RAMEN_LOG -> ramenLogService::removeByModerator;
            case RAMEN_LOG_COMMENT -> commentService::removeByModerator;
        };
        remove.accept(report.getTargetId());
    }

    private Previews findPreviews(List<MobileContentReport> page) {
        Set<Long> logIds = new HashSet<>();
        Set<Long> commentIds = new HashSet<>();
        for (MobileContentReport report : page) {
            Set<Long> targetIds = switch (report.getTargetType()) {
                case RAMEN_LOG -> logIds;
                case RAMEN_LOG_COMMENT -> commentIds;
            };
            targetIds.add(report.getTargetId());
        }
        Map<Long, MobileRamenLogComment> commentById = commentRepository.findAllById(commentIds).stream()
                .collect(Collectors.toMap(MobileRamenLogComment::getId, Function.identity()));
        Map<Long, MobileRamenLog> logById = ramenLogRepository.findAllById(logIds).stream()
                .collect(Collectors.toMap(MobileRamenLog::getId, Function.identity()));
        Map<Long, String> firstImage = new HashMap<>();
        for (MobileRamenLogImage image : imageRepository.findByRamenLogIdInOrderByRamenLogIdAscSortOrderAsc(logIds)) {
            firstImage.putIfAbsent(image.getRamenLogId(), image.getUrl());
        }
        Set<Long> userIds = new HashSet<>();
        page.forEach(report -> userIds.add(report.getReporterId()));
        logById.values().forEach(log -> userIds.add(log.getUserId()));
        commentById.values().forEach(comment -> userIds.add(comment.getUserId()));
        return new Previews(
                logById,
                commentById,
                firstImage,
                accountFacade.authors(userIds),
                countOpenReports(ReportTargetType.RAMEN_LOG, logIds),
                countOpenReports(ReportTargetType.RAMEN_LOG_COMMENT, commentIds));
    }

    private Map<Long, Integer> countOpenReports(ReportTargetType type, Set<Long> targetIds) {
        Map<Long, Integer> counts = new HashMap<>();
        if (targetIds.isEmpty()) {
            return counts;
        }
        for (Object[] row : contentReportRepository.countOpenByTarget(type, targetIds)) {
            counts.put((Long) row[0], ((Number) row[1]).intValue());
        }
        return counts;
    }

    private MobileAdminReportResult toReportResult(MobileContentReport report, Previews previews) {
        Long targetId = report.getTargetId();
        TargetPreview preview = switch (report.getTargetType()) {
            case RAMEN_LOG ->
                new TargetPreview(
                        toLogTarget(targetId, previews),
                        previews.openLogReports().getOrDefault(targetId, 0));
            case RAMEN_LOG_COMMENT ->
                new TargetPreview(
                        toCommentTarget(targetId, previews),
                        previews.openCommentReports().getOrDefault(targetId, 0));
        };
        return new MobileAdminReportResult(
                String.valueOf(report.getId()),
                report.getStatus(),
                report.getReason(),
                toPerson(report.getReporterId(), previews.authors()),
                report.getCreatedAt(),
                report.getResolvedAt(),
                preview.openCount(),
                preview.target());
    }

    private Target toLogTarget(Long logId, Previews previews) {
        MobileRamenLog log = previews.logs().get(logId);
        if (log == null) {
            return toMissingTarget(ReportTargetType.RAMEN_LOG, logId, logId);
        }
        return new Target(
                ReportTargetType.RAMEN_LOG,
                String.valueOf(logId),
                String.valueOf(logId),
                toPerson(log.getUserId(), previews.authors()),
                log.getNote(),
                log.getMenuName(),
                previews.firstImages().get(logId),
                log.getDeletedAt() != null);
    }

    private Target toCommentTarget(Long commentId, Previews previews) {
        MobileRamenLogComment comment = previews.comments().get(commentId);
        if (comment == null) {
            return toMissingTarget(ReportTargetType.RAMEN_LOG_COMMENT, commentId, null);
        }
        return new Target(
                ReportTargetType.RAMEN_LOG_COMMENT,
                String.valueOf(commentId),
                String.valueOf(comment.getRamenLogId()),
                toPerson(comment.getUserId(), previews.authors()),
                comment.getContent(),
                null,
                null,
                comment.getDeletedAt() != null);
    }

    /** 대상 행이 없으면(정상 흐름에서는 탈퇴 정리 때 신고도 함께 지워진다) 지워진 대상으로 보여 준다. */
    private static Target toMissingTarget(ReportTargetType type, Long targetId, Long ramenLogId) {
        String logId = null;
        if (ramenLogId != null) {
            logId = String.valueOf(ramenLogId);
        }
        return new Target(type, String.valueOf(targetId), logId, null, null, null, null, true);
    }

    private static Person toPerson(Long userId, Map<Long, MobileAuthor> authors) {
        MobileAuthor author = authors.get(userId);
        String nickname = null;
        if (author != null) {
            nickname = author.nickname();
        }
        return new Person(String.valueOf(userId), nickname);
    }

    private static MobileException alreadyClosed() {
        return new MobileException(MobileErrorCode.CONFLICT, "이미 처리된 신고입니다.");
    }

    private record Previews(
            Map<Long, MobileRamenLog> logs,
            Map<Long, MobileRamenLogComment> comments,
            Map<Long, String> firstImages,
            Map<Long, MobileAuthor> authors,
            Map<Long, Integer> openLogReports,
            Map<Long, Integer> openCommentReports) {}

    private record TargetPreview(Target target, int openCount) {}
}
