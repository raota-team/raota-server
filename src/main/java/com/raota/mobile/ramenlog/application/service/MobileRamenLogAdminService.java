package com.raota.mobile.ramenlog.application.service;

import com.raota.mobile.account.application.facade.MobileAccountRamenLogFacade;
import com.raota.mobile.account.application.facade.MobileAccountRamenLogFacade.MobileAuthor;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import com.raota.mobile.common.page.OffsetPage;
import com.raota.mobile.ramenlog.application.result.MobileAdminRamenLogCountsResult;
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
import com.raota.mobile.ramenlog.domain.model.RevisitIntention;
import com.raota.mobile.ramenlog.domain.repository.MobileContentReportRepository;
import com.raota.mobile.ramenlog.domain.repository.MobileRamenLogImageRepository;
import com.raota.mobile.ramenlog.domain.repository.MobileRamenLogRepository;
import com.raota.mobile.ramenlog.domain.repository.MobileRamenLogRepository.VisibilityCounts;
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

/**
 * 관리자 웹의 앱 기록 조회와 운영자 삭제다. 삭제된 기록은 보여 주지 않고, 비공개 기록은 그 순간의 공개 범위를 보고 ID·작성일·작성자만 보여 준다.
 */
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
            LogVisibility visibility, String keyword, Long userId, Long shopId, int page, int size) {
        Page<MobileRamenLog> found = ramenLogRepository.findForAdmin(
                visibility,
                blankToNull(keyword),
                userId,
                shopId,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        Related related = findRelated(found.getContent());
        return OffsetPage.of(found, ramenLog -> toRow(ramenLog, related));
    }

    @Transactional(readOnly = true)
    public MobileAdminRamenLogDetailResult detail(Long logId) {
        MobileRamenLog ramenLog = ramenLogRepository
                .findById(logId)
                .filter(found -> found.getDeletedAt() == null)
                .orElseThrow(MobileRamenLogAdminService::notFound);
        Related related = findRelated(List.of(ramenLog));
        MobileAdminRamenLogResult row = toRow(ramenLog, related);
        if (isHidden(ramenLog)) {
            return toDetail(row, null, null, null, null);
        }
        return toDetail(
                row,
                related.imageUrls().getOrDefault(logId, List.of()),
                toTasteNotes(ramenLog.getTasteNoteCodes()),
                ramenLog.getRevisitIntention(),
                toScores(ramenLog));
    }

    @Transactional(readOnly = true)
    public MobileAdminRamenLogCountsResult counts(Long userId) {
        VisibilityCounts counts = ramenLogRepository.countLiveByVisibility(userId);
        return new MobileAdminRamenLogCountsResult(
                String.valueOf(userId), counts.getPublicCount(), counts.getPrivateCount());
    }

    /** 작성자 삭제와 같이 집계를 줄이고, 이 기록에 열린 신고는 삭제로 처리된 것으로 닫는다. */
    @Transactional
    public MobileAdminRamenLogDeletionResult delete(Long logId) {
        // 잠근 채로 판단해 동시에 지운 두 관리자 중 한 명만 성공하고, 작성자가 그 사이 비공개로 바꾼 기록은 지우지 않는다.
        MobileRamenLog ramenLog = ramenLogRepository
                .findByIdForUpdate(logId)
                .filter(found -> found.getDeletedAt() == null)
                .orElseThrow(MobileRamenLogAdminService::notFound);
        if (isHidden(ramenLog)) {
            throw privateLogNotDeletable();
        }
        ramenLogService.removeByModerator(logId);
        int closed = contentReportRepository.closeOpen(
                ReportTargetType.RAMEN_LOG,
                logId,
                ReportStatus.RESOLVED,
                Instant.now(clock).truncatedTo(ChronoUnit.MICROS));
        return new MobileAdminRamenLogDeletionResult(closed);
    }

    /** 신고 처리도 같은 규칙으로 막는다. */
    static MobileException privateLogNotDeletable() {
        return new MobileException(MobileErrorCode.FORBIDDEN, "비공개 기록은 관리자가 삭제할 수 없습니다.");
    }

    private static boolean isHidden(MobileRamenLog ramenLog) {
        return ramenLog.getVisibility() != LogVisibility.PUBLIC;
    }

    /** 비공개 기록의 사진·매장·신고는 읽지 않는다. */
    private Related findRelated(Collection<MobileRamenLog> logs) {
        Set<Long> logIds = new HashSet<>();
        Set<Long> userIds = new HashSet<>();
        Set<Long> shopIds = new HashSet<>();
        for (MobileRamenLog ramenLog : logs) {
            userIds.add(ramenLog.getUserId());
            if (!isHidden(ramenLog)) {
                logIds.add(ramenLog.getId());
                shopIds.add(ramenLog.getShopId());
            }
        }
        Map<Long, List<String>> imageUrls = new HashMap<>();
        Map<Long, Integer> openReports = new HashMap<>();
        Map<Long, MobileShopRef> shops = Map.of();
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
            shops = shopFacade.findShopRefs(shopIds);
        }
        return new Related(accountFacade.authors(userIds), shops, imageUrls, openReports);
    }

    private static MobileAdminRamenLogResult toRow(MobileRamenLog ramenLog, Related related) {
        Long logId = ramenLog.getId();
        Author author = toAuthor(ramenLog.getUserId(), related.authors());
        if (isHidden(ramenLog)) {
            return new MobileAdminRamenLogResult(
                    String.valueOf(logId),
                    ramenLog.getVisibility(),
                    ramenLog.getCreatedAt(),
                    author,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
        }
        List<String> images = related.imageUrls().getOrDefault(logId, List.of());
        String thumbnailUrl = null;
        if (!images.isEmpty()) {
            thumbnailUrl = images.getFirst();
        }
        return new MobileAdminRamenLogResult(
                String.valueOf(logId),
                ramenLog.getVisibility(),
                ramenLog.getCreatedAt(),
                author,
                toShop(ramenLog.getShopId(), related.shops()),
                ramenLog.getMenuName(),
                ramenLog.getRamenType(),
                ramenLog.getVisitedAt(),
                ramenLog.getNote(),
                thumbnailUrl,
                images.size(),
                ramenLog.getLikeCount(),
                ramenLog.getCommentCount(),
                related.openReports().getOrDefault(logId, 0),
                ramenLog.getUpdatedAt());
    }

    private static MobileAdminRamenLogDetailResult toDetail(
            MobileAdminRamenLogResult row,
            List<String> imageUrls,
            List<MobileAdminRamenLogDetailResult.TasteNote> tasteNotes,
            RevisitIntention revisitIntention,
            MobileAdminRamenLogDetailResult.Scores scores) {
        return new MobileAdminRamenLogDetailResult(
                row.id(),
                row.visibility(),
                row.createdAt(),
                row.author(),
                row.shop(),
                row.menuName(),
                row.ramenType(),
                row.visitedAt(),
                row.note(),
                row.thumbnailUrl(),
                row.imageCount(),
                row.likeCount(),
                row.commentCount(),
                row.openReportCount(),
                row.updatedAt(),
                imageUrls,
                tasteNotes,
                revisitIntention,
                scores);
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
