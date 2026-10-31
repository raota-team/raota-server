package com.raota.mobile.ramenlog.application.service;

import com.raota.mobile.account.application.facade.MobileAccountRamenLogFacade;
import com.raota.mobile.account.application.facade.MobileAccountRamenLogFacade.MobileAuthor;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import com.raota.mobile.common.page.OffsetPage;
import com.raota.mobile.ramenlog.application.query.MobileAdminRamenLogState;
import com.raota.mobile.ramenlog.application.result.MobileAdminRamenLogDeletionResult;
import com.raota.mobile.ramenlog.application.result.MobileAdminRamenLogDetailResult;
import com.raota.mobile.ramenlog.application.result.MobileAdminRamenLogResult;
import com.raota.mobile.ramenlog.application.result.MobileAdminRamenLogResult.Author;
import com.raota.mobile.ramenlog.application.result.MobileAdminRamenLogResult.Shop;
import com.raota.mobile.ramenlog.domain.model.LogVisibility;
import com.raota.mobile.ramenlog.domain.model.MobileRamenLog;
import com.raota.mobile.ramenlog.domain.model.MobileRamenLogImage;
import com.raota.mobile.ramenlog.domain.model.MobileTasteNote;
import com.raota.mobile.ramenlog.domain.model.ReportStatus;
import com.raota.mobile.ramenlog.domain.model.ReportTargetType;
import com.raota.mobile.ramenlog.domain.repository.MobileContentReportRepository;
import com.raota.mobile.ramenlog.domain.repository.MobileRamenLogImageRepository;
import com.raota.mobile.ramenlog.domain.repository.MobileRamenLogRepository;
import com.raota.mobile.shop.application.facade.MobileShopRamenLogFacade;
import com.raota.mobile.shop.application.facade.MobileShopRamenLogFacade.MobileShopRef;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 웹의 앱 기록 조회와 운영자 삭제다. 공개 범위·작성자 상태와 관계없이 모든 기록을 보여 준다. */
@Service
@RequiredArgsConstructor
public class MobileRamenLogAdminService {

    private final MobileRamenLogRepository ramenLogRepository;

    private final MobileRamenLogImageRepository imageRepository;

    private final MobileContentReportRepository contentReportRepository;

    private final MobileRamenLogService ramenLogService;

    private final MobileAccountRamenLogFacade accountFacade;

    private final MobileShopRamenLogFacade shopFacade;

    private final Clock clock;

    /** 필터는 모두 선택값이고 null이면 보지 않는다. 최근 작성한 기록부터 보여 준다. */
    @Transactional(readOnly = true)
    public OffsetPage<MobileAdminRamenLogResult> list(
            LogVisibility visibility,
            MobileAdminRamenLogState state,
            String keyword,
            Long userId,
            Long shopId,
            int page,
            int size) {
        Page<MobileRamenLog> found = ramenLogRepository.findForAdmin(
                visibility,
                deletedFilter(state),
                blankToNull(keyword),
                userId,
                shopId,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        Related related = findRelated(found.getContent());
        return OffsetPage.of(found, ramenLog -> toRow(ramenLog, related));
    }

    /** 삭제된 기록도 돌려준다. */
    @Transactional(readOnly = true)
    public MobileAdminRamenLogDetailResult detail(Long logId) {
        MobileRamenLog ramenLog = ramenLogRepository.findById(logId).orElseThrow(MobileRamenLogAdminService::notFound);
        Related related = findRelated(List.of(ramenLog));
        MobileAdminRamenLogResult row = toRow(ramenLog, related);
        return new MobileAdminRamenLogDetailResult(
                row.id(),
                row.author(),
                row.shop(),
                row.menuName(),
                row.ramenType(),
                row.visitedAt(),
                row.visibility(),
                row.note(),
                row.thumbnailUrl(),
                row.imageCount(),
                row.likeCount(),
                row.commentCount(),
                row.openReportCount(),
                row.createdAt(),
                row.updatedAt(),
                row.deletedAt(),
                related.imageUrls().getOrDefault(logId, List.of()),
                toTasteNotes(ramenLog.getTasteNoteCodes()),
                ramenLog.getRevisitIntention(),
                toScores(ramenLog));
    }

    /** 작성자 삭제와 같이 집계를 줄이고, 이 기록에 열린 신고는 삭제로 처리된 것으로 닫는다. */
    @Transactional
    public MobileAdminRamenLogDeletionResult delete(Long logId) {
        // 잠근 채로 삭제 여부를 확인해 동시에 지운 두 관리자 중 한 명만 성공하게 한다.
        MobileRamenLog ramenLog =
                ramenLogRepository.findByIdForUpdate(logId).orElseThrow(MobileRamenLogAdminService::notFound);
        if (ramenLog.getDeletedAt() != null) {
            throw new MobileException(MobileErrorCode.CONFLICT, "이미 삭제된 기록입니다.");
        }
        ramenLogService.removeByModerator(logId);
        int closed = contentReportRepository.closeOpen(
                ReportTargetType.RAMEN_LOG,
                logId,
                ReportStatus.RESOLVED,
                Instant.now(clock).truncatedTo(ChronoUnit.MICROS));
        return new MobileAdminRamenLogDeletionResult(closed);
    }

    private Related findRelated(Collection<MobileRamenLog> logs) {
        Set<Long> logIds = new HashSet<>();
        Set<Long> userIds = new HashSet<>();
        Set<Long> shopIds = new HashSet<>();
        for (MobileRamenLog ramenLog : logs) {
            logIds.add(ramenLog.getId());
            userIds.add(ramenLog.getUserId());
            shopIds.add(ramenLog.getShopId());
        }
        Map<Long, List<String>> imageUrls = new HashMap<>();
        Map<Long, Integer> openReports = new HashMap<>();
        if (!logIds.isEmpty()) {
            for (MobileRamenLogImage image :
                    imageRepository.findByRamenLogIdInOrderByRamenLogIdAscSortOrderAsc(logIds)) {
                imageUrls
                        .computeIfAbsent(image.getRamenLogId(), id -> new ArrayList<>())
                        .add(image.getUrl());
            }
            for (Object[] row : contentReportRepository.countOpenByTarget(ReportTargetType.RAMEN_LOG, logIds)) {
                openReports.put((Long) row[0], ((Number) row[1]).intValue());
            }
        }
        return new Related(accountFacade.authors(userIds), shopFacade.findShopRefs(shopIds), imageUrls, openReports);
    }

    private static MobileAdminRamenLogResult toRow(MobileRamenLog ramenLog, Related related) {
        Long logId = ramenLog.getId();
        List<String> images = related.imageUrls().getOrDefault(logId, List.of());
        String thumbnailUrl = null;
        if (!images.isEmpty()) {
            thumbnailUrl = images.getFirst();
        }
        return new MobileAdminRamenLogResult(
                String.valueOf(logId),
                toAuthor(ramenLog.getUserId(), related.authors()),
                toShop(ramenLog.getShopId(), related.shops()),
                ramenLog.getMenuName(),
                ramenLog.getRamenType(),
                ramenLog.getVisitedAt(),
                ramenLog.getVisibility(),
                ramenLog.getNote(),
                thumbnailUrl,
                images.size(),
                ramenLog.getLikeCount(),
                ramenLog.getCommentCount(),
                related.openReports().getOrDefault(logId, 0),
                ramenLog.getCreatedAt(),
                ramenLog.getUpdatedAt(),
                ramenLog.getDeletedAt());
    }

    private static Author toAuthor(Long userId, Map<Long, MobileAuthor> authors) {
        MobileAuthor author = authors.get(userId);
        String nickname = null;
        if (author != null) {
            nickname = author.nickname();
        }
        return new Author(String.valueOf(userId), nickname);
    }

    private static Shop toShop(Long shopId, Map<Long, MobileShopRef> shops) {
        MobileShopRef shop = shops.get(shopId);
        if (shop == null) {
            return new Shop(String.valueOf(shopId), null, null);
        }
        return new Shop(String.valueOf(shopId), shop.name(), shop.branchName());
    }

    private static List<MobileAdminRamenLogDetailResult.TasteNote> toTasteNotes(List<String> codes) {
        return codes.stream()
                .map(code -> {
                    if (!MobileTasteNote.isCode(code)) {
                        return new MobileAdminRamenLogDetailResult.TasteNote(code, null, null);
                    }
                    MobileTasteNote note = MobileTasteNote.valueOf(code);
                    return new MobileAdminRamenLogDetailResult.TasteNote(code, note.category(), note.label());
                })
                .toList();
    }

    private static MobileAdminRamenLogDetailResult.Scores toScores(MobileRamenLog ramenLog) {
        if (ramenLog.getSatisfactionScore() == null) {
            return null;
        }
        return new MobileAdminRamenLogDetailResult.Scores(
                ramenLog.getSatisfactionScore(),
                ramenLog.getBrothDensityScore(),
                ramenLog.getNoodleFirmnessScore(),
                ramenLog.getToppingScore());
    }

    /** null이면 삭제 여부를 보지 않는다. */
    private static Boolean deletedFilter(MobileAdminRamenLogState state) {
        return switch (state) {
            case LIVE -> false;
            case DELETED -> true;
            case ALL -> null;
        };
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }

    private static MobileException notFound() {
        return new MobileException(MobileErrorCode.RESOURCE_NOT_FOUND, "기록을 찾을 수 없습니다.");
    }

    private record Related(
            Map<Long, MobileAuthor> authors,
            Map<Long, MobileShopRef> shops,
            Map<Long, List<String>> imageUrls,
            Map<Long, Integer> openReports) {}
}
