package com.raota.mobile.ramenlog.application.service;

import com.raota.mobile.account.application.facade.MobileAccountRamenLogFacade;
import com.raota.mobile.account.application.facade.MobileAccountRamenLogFacade.MobileAuthor;
import com.raota.mobile.common.cursor.Cursor;
import com.raota.mobile.common.cursor.CursorPage;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import com.raota.mobile.ramenlog.application.query.MobileRamenLogSort;
import com.raota.mobile.ramenlog.application.result.MobileRamenLogSummary;
import com.raota.mobile.ramenlog.application.result.MobileRamenLogSummaryItem;
import com.raota.mobile.ramenlog.application.result.MobileTasteNoteDefinitions;
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

    private static final MobileTasteNoteDefinitions DEFINITIONS = definitionsFromEnum();

    /** 걸러낸 기록을 채우려고 한 요청에서 더 읽는 묶음 수의 상한이다. 요청당 최대 {@code 5 × (size + 1)}행을 읽는다. */
    static final int MAX_FEED_BATCHES = 5;

    private final MobileRamenLogRepository logs;

    private final MobileRamenLogImageRepository images;

    private final MobileShopRamenLogFacade shops;

    private final MobileAccountRamenLogFacade accounts;

    private final MobileRamenLogLikeRepository likes;

    @Transactional(readOnly = true)
    public CursorPage<MobileRamenLogSummary> list(Long userId, String rawCursor, int size) {
        Cursor cursor = Cursor.parse(rawCursor).orElse(null);
        LocalDate after = parseDate(cursor);
        List<MobileRamenLog> fetched = logs.findMine(userId, after, cursor == null ? null : cursor.id(),
                PageRequest.of(0, size + 1));
        CursorPage<MobileRamenLog> page = CursorPage.of(fetched, size,
                log -> Cursor.of(log.getVisitedAt(), log.getId()));
        return summaries(page, accounts.authors(List.of(userId)), userId);
    }

    /**
     * 공개 기록을 정렬 기준의 커서로 읽는다. 활성 회원이 아닌 작성자(탈퇴 유예·정지)의 기록은 건너뛰고, 걸러낸 만큼 다음 묶음을 더 읽어 페이지를
     * 채운다. 묶음 상한에 닿으면 요청 크기보다 짧은 페이지와 마지막으로 읽은 위치의 커서를 돌려준다.
     */
    @Transactional(readOnly = true)
    public CursorPage<MobileRamenLogSummary> feed(Long viewerId, MobileRamenLogSort sort, String ramenType, Long shopId,
            String rawCursor, int size) {
        Cursor after = Cursor.parse(rawCursor).orElse(null);
        List<MobileRamenLog> visible = new ArrayList<>();
        Map<Long, MobileAuthor> authors = new HashMap<>();
        for (int batches = 1;; batches++) {
            List<MobileRamenLog> batch = feedBatch(sort, ramenType, shopId, after, size + 1);
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
    public List<MobileRamenLogSummaryItem> summary(Long userId) {
        List<MobileRamenLog> newest = logs.findMine(userId, null, null, PageRequest.of(0, SUMMARY_LIMIT));
        if (newest.isEmpty()) {
            return List.of();
        }
        Map<Long, MobileShopRef> shopRefs = shopRefs(newest);
        return newest.stream()
            .map(log -> MobileRamenLogViews.summaryItem(log, requiredShop(shopRefs, log.getShopId())))
            .toList();
    }

    public MobileTasteNoteDefinitions definitions() {
        return DEFINITIONS;
    }

    private CursorPage<MobileRamenLogSummary> summaries(CursorPage<MobileRamenLog> page,
            Map<Long, MobileAuthor> authors, Long viewerId) {
        if (page.items().isEmpty()) {
            return new CursorPage<>(List.of(), page.nextCursor(), page.hasNext());
        }
        List<Long> ids = page.items().stream().map(MobileRamenLog::getId).toList();
        Map<Long, MobileShopRef> shopRefs = shopRefs(page.items());
        Map<Long, List<String>> urls = new HashMap<>();
        for (MobileRamenLogImage image : images.findByRamenLogIdInOrderByRamenLogIdAscSortOrderAsc(ids)) {
            urls.computeIfAbsent(image.getRamenLogId(), ignored -> new ArrayList<>()).add(image.getUrl());
        }
        Set<Long> liked = viewerId == null ? Set.of() : new HashSet<>(likes.findLikedLogIds(viewerId, ids));
        List<MobileRamenLogSummary> items = page.items()
            .stream()
            .map(log -> MobileRamenLogViews.summary(log, authors.get(log.getUserId()),
                    requiredShop(shopRefs, log.getShopId()), urls.getOrDefault(log.getId(), List.of()),
                    liked.contains(log.getId()), viewerId))
            .toList();
        return new CursorPage<>(items, page.nextCursor(), page.hasNext());
    }

    private List<MobileRamenLog> feedBatch(MobileRamenLogSort sort, String ramenType, Long shopId, Cursor after,
            int limit) {
        Long afterId = after == null ? null : after.id();
        PageRequest page = PageRequest.of(0, limit);
        return switch (sort) {
            case LATEST -> logs.findLatest(LogVisibility.PUBLIC, ramenType, shopId,
                    after == null ? null : parseInstant(after), afterId, page);
            case LIKES -> logs.findMostLiked(LogVisibility.PUBLIC, ramenType, shopId,
                    after == null ? null : parseLikes(after), afterId, page);
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
        }
        catch (DateTimeParseException exception) {
            throw invalidCursor();
        }
    }

    private Instant parseInstant(Cursor cursor) {
        try {
            return Instant.parse(cursor.sortValue());
        }
        catch (DateTimeParseException exception) {
            throw invalidCursor();
        }
    }

    private Integer parseLikes(Cursor cursor) {
        try {
            return Integer.valueOf(cursor.sortValue());
        }
        catch (NumberFormatException exception) {
            throw invalidCursor();
        }
    }

    private MobileException invalidCursor() {
        return new MobileException(MobileErrorCode.VALIDATION_ERROR, "커서가 올바르지 않습니다.");
    }

    private static MobileTasteNoteDefinitions definitionsFromEnum() {
        Map<String, List<MobileTasteNoteDefinitions.Note>> groups = new LinkedHashMap<>();
        for (MobileTasteNote note : MobileTasteNote.values()) {
            groups.computeIfAbsent(note.category(), ignored -> new ArrayList<>())
                .add(new MobileTasteNoteDefinitions.Note(note.name(), note.label()));
        }
        return new MobileTasteNoteDefinitions("1",
                groups.entrySet()
                    .stream()
                    .map(entry -> new MobileTasteNoteDefinitions.Group(entry.getKey(), List.copyOf(entry.getValue())))
                    .toList());
    }

}
