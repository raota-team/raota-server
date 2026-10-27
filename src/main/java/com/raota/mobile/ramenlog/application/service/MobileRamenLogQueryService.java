package com.raota.mobile.ramenlog.application.service;

import com.raota.mobile.account.application.facade.MobileAccountRamenLogFacade;
import com.raota.mobile.account.application.facade.MobileAccountRamenLogFacade.MobileAuthor;
import com.raota.mobile.common.cursor.Cursor;
import com.raota.mobile.common.cursor.CursorPage;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import com.raota.mobile.ramenlog.application.query.MobileRamenLogSort;
import com.raota.mobile.ramenlog.application.result.MobileRamenLogSummaryItemResult;
import com.raota.mobile.ramenlog.application.result.MobileRamenLogSummaryResult;
import com.raota.mobile.ramenlog.application.result.MobileTasteNoteDefinitionsResult;
import com.raota.mobile.ramenlog.domain.model.LogVisibility;
import com.raota.mobile.ramenlog.domain.model.MobileRamenLog;
import com.raota.mobile.ramenlog.domain.model.MobileRamenLogImage;
import com.raota.mobile.ramenlog.domain.model.MobileTasteNote;
import com.raota.mobile.ramenlog.domain.repository.MobileRamenLogImageRepository;
import com.raota.mobile.ramenlog.domain.repository.MobileRamenLogLikeRepository;
import com.raota.mobile.ramenlog.domain.repository.MobileRamenLogRepository;
import com.raota.mobile.shop.application.facade.MobileShopRamenLogFacade;
import com.raota.mobile.shop.application.facade.MobileShopRamenLogFacade.MobileShopRef;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 방문일·ID 순으로 내 기록을 읽고 연관 데이터를 페이지 단위로 합친다. */
@Service
@RequiredArgsConstructor
public class MobileRamenLogQueryService {

    private static final int SUMMARY_LIMIT = 2000;

    private static final MobileTasteNoteDefinitionsResult DEFINITIONS = definitionsFromEnum();

    /** 걸러낸 기록을 채우려고 한 요청에서 더 읽는 묶음 수의 상한이다. 요청당 최대 {@code 5 × (size + 1)}행을 읽는다. */
    static final int MAX_FEED_BATCHES = 5;

    private final MobileRamenLogRepository logs;

    private final MobileRamenLogImageRepository images;

    private final MobileShopRamenLogFacade shops;

    private final MobileAccountRamenLogFacade accounts;

    private final MobileRamenLogLikeRepository likes;

    /** 내 기록(비공개 포함)을 방문일·ID 역순으로 읽는다. {@code ramenType}이 오면 그 종류만이다. */
    @Transactional(readOnly = true)
    public CursorPage<MobileRamenLogSummaryResult> list(Long userId, String ramenType, String rawCursor, int size) {
        Cursor cursor = Cursor.parse(rawCursor).orElse(null);
        LocalDate afterDate = null;
        Long afterId = null;
        if (cursor != null) {
            afterDate = parseDate(cursor);
            afterId = cursor.id();
        }
        List<MobileRamenLog> fetched =
                logs.findMine(userId, ramenType, afterDate, afterId, PageRequest.of(0, size + 1));
        CursorPage<MobileRamenLog> page =
                CursorPage.of(fetched, size, log -> Cursor.of(log.getVisitedAt(), log.getId()));
        return summaries(page, accounts.authors(List.of(userId)), userId);
    }

    /**
     * 공개 기록을 정렬 기준의 커서로 읽는다. 로그인한 회원이 차단한 작성자와 신고한 기록은 쿼리에서 빼고, 활성 회원이 아닌 작성자(탈퇴 유예·정지)의
     * 기록은 건너뛴 뒤 걸러낸 만큼 다음 묶음을 더 읽어 페이지를 채운다. 묶음 상한에 닿으면 요청 크기보다 짧은 페이지와 마지막으로 읽은 위치의 커서를
     * 돌려준다.
     */
    @Transactional(readOnly = true)
    public CursorPage<MobileRamenLogSummaryResult> feed(
            Long viewerId, MobileRamenLogSort sort, String ramenType, Long shopId, String rawCursor, int size) {
        Cursor after = Cursor.parse(rawCursor).orElse(null);
        List<MobileRamenLog> visible = new ArrayList<>();
        Map<Long, MobileAuthor> authors = new HashMap<>();
        for (int batches = 1; ; batches++) {
            List<MobileRamenLog> batch = feedBatch(sort, ramenType, shopId, viewerId, after, size + 1);
            Set<Long> unknown = batch.stream()
                    .map(MobileRamenLog::getUserId)
                    .filter(id -> !authors.containsKey(id))
                    .collect(Collectors.toSet());
            authors.putAll(accounts.authors(unknown));
            for (MobileRamenLog log : batch) {
                MobileAuthor author = authors.get(log.getUserId());
                if (author != null && author.active()) {
                    visible.add(log);
                }
            }
            if (visible.size() > size || batch.size() <= size) {
                break;
            }
            after = feedCursor(sort, batch.getLast());
            if (batches == MAX_FEED_BATCHES) {
                return summaries(new CursorPage<>(visible, after.encode(), true), authors, viewerId);
            }
        }
        CursorPage<MobileRamenLog> page = CursorPage.of(visible, size, log -> feedCursor(sort, log));
        return summaries(page, authors, viewerId);
    }

    @Transactional(readOnly = true)
    public List<MobileRamenLogSummaryItemResult> summary(Long userId) {
        List<MobileRamenLog> newest = logs.findMine(userId, null, null, null, PageRequest.of(0, SUMMARY_LIMIT));
        if (newest.isEmpty()) {
            return List.of();
        }
        Map<Long, MobileShopRef> shopRefs = shopRefs(newest);
        return newest.stream()
                .map(log -> MobileRamenLogResultAssembler.toSummaryItem(log, requiredShop(shopRefs, log.getShopId())))
                .toList();
    }

    public MobileTasteNoteDefinitionsResult definitions() {
        return DEFINITIONS;
    }

    private CursorPage<MobileRamenLogSummaryResult> summaries(
            CursorPage<MobileRamenLog> page, Map<Long, MobileAuthor> authors, Long viewerId) {
        if (page.items().isEmpty()) {
            return new CursorPage<>(List.of(), page.nextCursor(), page.hasNext());
        }
        List<Long> ids = page.items().stream().map(MobileRamenLog::getId).toList();
        Map<Long, MobileShopRef> shopRefs = shopRefs(page.items());
        Map<Long, List<String>> urls = new HashMap<>();
        for (MobileRamenLogImage image : images.findByRamenLogIdInOrderByRamenLogIdAscSortOrderAsc(ids)) {
            urls.computeIfAbsent(image.getRamenLogId(), ignored -> new ArrayList<>())
                    .add(image.getUrl());
        }
        Set<Long> liked = likedLogIds(viewerId, ids);
        List<MobileRamenLogSummaryResult> items = page.items().stream()
                .map(log -> MobileRamenLogResultAssembler.toSummary(
                        log,
                        authors.get(log.getUserId()),
                        requiredShop(shopRefs, log.getShopId()),
                        urls.getOrDefault(log.getId(), List.of()),
                        liked.contains(log.getId()),
                        viewerId))
                .toList();
        return new CursorPage<>(items, page.nextCursor(), page.hasNext());
    }

    /** 비회원은 공감한 기록이 없다. */
    private Set<Long> likedLogIds(Long viewerId, List<Long> ids) {
        if (viewerId == null) {
            return Set.of();
        }
        return new HashSet<>(likes.findLikedLogIds(viewerId, ids));
    }

    private List<MobileRamenLog> feedBatch(
            MobileRamenLogSort sort, String ramenType, Long shopId, Long viewerId, Cursor after, int limit) {
        PageRequest page = PageRequest.of(0, limit);
        if (after == null) {
            return switch (sort) {
                case LATEST -> logs.findLatest(LogVisibility.PUBLIC, ramenType, shopId, viewerId, null, null, page);
                case LIKES -> logs.findMostLiked(LogVisibility.PUBLIC, ramenType, shopId, viewerId, null, null, page);
            };
        }
        return switch (sort) {
            case LATEST ->
                logs.findLatest(
                        LogVisibility.PUBLIC, ramenType, shopId, viewerId, parseInstant(after), after.id(), page);
            case LIKES ->
                logs.findMostLiked(
                        LogVisibility.PUBLIC, ramenType, shopId, viewerId, parseLikes(after), after.id(), page);
        };
    }

    private Cursor feedCursor(MobileRamenLogSort sort, MobileRamenLog log) {
        return switch (sort) {
            case LATEST -> Cursor.of(log.getCreatedAt(), log.getId());
            case LIKES -> Cursor.of(log.getLikeCount(), log.getId());
        };
    }

    private Map<Long, MobileShopRef> shopRefs(List<MobileRamenLog> source) {
        Set<Long> shopIds = source.stream().map(MobileRamenLog::getShopId).collect(Collectors.toSet());
        return shops.findShopRefs(shopIds);
    }

    private MobileShopRef requiredShop(Map<Long, MobileShopRef> shopRefs, Long shopId) {
        MobileShopRef shop = shopRefs.get(shopId);
        if (shop == null) {
            throw new MobileException(MobileErrorCode.RESOURCE_NOT_FOUND, "매장을 찾을 수 없습니다.");
        }
        return shop;
    }

    private LocalDate parseDate(Cursor cursor) {
        if (cursor == null) {
            return null;
        }
        try {
            return LocalDate.parse(cursor.sortValue());
        } catch (DateTimeParseException exception) {
            throw invalidCursor();
        }
    }

    private Instant parseInstant(Cursor cursor) {
        try {
            return Instant.parse(cursor.sortValue());
        } catch (DateTimeParseException exception) {
            throw invalidCursor();
        }
    }

    private Integer parseLikes(Cursor cursor) {
        try {
            return Integer.valueOf(cursor.sortValue());
        } catch (NumberFormatException exception) {
            throw invalidCursor();
        }
    }

    private MobileException invalidCursor() {
        return new MobileException(MobileErrorCode.VALIDATION_ERROR, "커서가 올바르지 않습니다.");
    }

    private static MobileTasteNoteDefinitionsResult definitionsFromEnum() {
        Map<String, List<MobileTasteNoteDefinitionsResult.Note>> groups = new LinkedHashMap<>();
        for (MobileTasteNote note : MobileTasteNote.values()) {
            groups.computeIfAbsent(note.category(), ignored -> new ArrayList<>())
                    .add(new MobileTasteNoteDefinitionsResult.Note(note.name(), note.label()));
        }
        return new MobileTasteNoteDefinitionsResult(
                "1",
                groups.entrySet().stream()
                        .map(entry -> new MobileTasteNoteDefinitionsResult.Group(
                                entry.getKey(), List.copyOf(entry.getValue())))
                        .toList());
    }
}
