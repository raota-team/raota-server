package com.raota.mobile.ramenlog.application.service;

import com.raota.mobile.account.application.facade.MobileAccountRamenLogFacade;
import com.raota.mobile.account.application.facade.MobileAccountRamenLogFacade.MobileAuthor;
import com.raota.mobile.common.cursor.Cursor;
import com.raota.mobile.common.cursor.CursorPage;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import com.raota.mobile.ramenlog.domain.model.MobileRamenLog;
import com.raota.mobile.ramenlog.domain.model.MobileRamenLogImage;
import com.raota.mobile.ramenlog.domain.model.MobileTasteNote;
import com.raota.mobile.ramenlog.domain.repository.MobileRamenLogImageRepository;
import com.raota.mobile.ramenlog.domain.repository.MobileRamenLogRepository;
import com.raota.mobile.ramenlog.application.result.MobileRamenLogSummary;
import com.raota.mobile.ramenlog.application.result.MobileRamenLogSummaryItem;
import com.raota.mobile.ramenlog.application.result.MobileTasteNoteDefinitions;
import com.raota.mobile.shop.application.facade.MobileShopRamenLogFacade;
import com.raota.mobile.shop.application.facade.MobileShopRamenLogFacade.MobileShopRef;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
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

    private final MobileRamenLogRepository logs;

    private final MobileRamenLogImageRepository images;

    private final MobileShopRamenLogFacade shops;

    private final MobileAccountRamenLogFacade accounts;

    @Transactional(readOnly = true)
    public CursorPage<MobileRamenLogSummary> list(Long userId, String rawCursor, int size) {
        Cursor cursor = Cursor.parse(rawCursor).orElse(null);
        LocalDate after = parseDate(cursor);
        List<MobileRamenLog> fetched = logs.findMine(userId, after, cursor == null ? null : cursor.id(),
                PageRequest.of(0, size + 1));
        CursorPage<MobileRamenLog> page = CursorPage.of(fetched, size,
                log -> Cursor.of(log.getVisitedAt(), log.getId()));
        if (page.items().isEmpty()) {
            return new CursorPage<>(List.of(), page.nextCursor(), page.hasNext());
        }
        List<Long> ids = page.items().stream().map(MobileRamenLog::getId).toList();
        Map<Long, MobileShopRef> shopRefs = shopRefs(page.items());
        MobileAuthor author = accounts.authors(List.of(userId)).get(userId);
        Map<Long, List<String>> urls = new HashMap<>();
        for (MobileRamenLogImage image : images.findByRamenLogIdInOrderByRamenLogIdAscSortOrderAsc(ids)) {
            urls.computeIfAbsent(image.getRamenLogId(), ignored -> new ArrayList<>()).add(image.getUrl());
        }
        List<MobileRamenLogSummary> items = page.items()
            .stream()
            .map(log -> MobileRamenLogViews.summary(log, author, requiredShop(shopRefs, log.getShopId()),
                    urls.getOrDefault(log.getId(), List.of())))
            .toList();
        return new CursorPage<>(items, page.nextCursor(), page.hasNext());
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
            throw new MobileException(MobileErrorCode.VALIDATION_ERROR, "커서가 올바르지 않습니다.");
        }
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
