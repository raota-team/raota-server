package com.raota.mobile.ramenlog.application.service;

import com.raota.mobile.account.application.facade.MobileAccountCopyFacade;
import com.raota.mobile.common.copy.LegacyCopyExclusion;
import com.raota.mobile.common.copy.LegacyCopyReport;
import com.raota.mobile.common.copy.LegacyCopyTally;
import com.raota.mobile.common.copy.LegacyKstTime;
import com.raota.mobile.ramenlog.application.port.MobileRamenLogCopyPort;
import com.raota.mobile.ramenlog.application.port.MobileRamenLogCopyPort.LegacyLike;
import com.raota.mobile.ramenlog.application.port.MobileRamenLogCopyPort.LegacyRamenLog;
import com.raota.mobile.ramenlog.application.port.MobileRamenLogCopyPort.LikeKey;
import com.raota.mobile.ramenlog.application.port.MobileRamenLogCopyPort.NewRamenLog;
import com.raota.mobile.ramenlog.domain.model.LogVisibility;
import com.raota.mobile.ramenlog.domain.model.MobileTasteNote;
import com.raota.mobile.ramenlog.domain.model.RevisitIntention;
import com.raota.mobile.shop.application.facade.MobileShopRamenLogFacade;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * v1 라멘 기록(사진 한 장 포함)과 공감을 ID를 유지해 v2로 복사하고 옮긴 기록의 회원·매장·공감 집계를 다시 맞춘다(#59). 회원 복사 뒤에
 * 실행하며, 이미 있는 행은 갱신하지 않고 건너뛴다.
 */
@Service
@RequiredArgsConstructor
public class MobileRamenLogCopyService {

    private static final String IDEMPOTENCY_KEY_PREFIX = "v1-ramen-log-";

    private static final String OTHER_RAMEN_TYPE = "기타";

    private static final int MENU_NAME_MAX_LENGTH = 150;

    private static final int NOTE_MAX_LENGTH = 500;

    /** v1 맛 태그 라벨 → v2 코드. v1에만 있던 "국물 흡착 좋아요"는 같은 뜻의 NOODLE_04("국물이 잘 배어요")로 옮긴다. */
    private static final Map<String, String> TASTE_CODES_BY_LABEL = tasteCodesByLabel();

    private final MobileRamenLogCopyPort port;

    private final MobileAccountCopyFacade accounts;

    private final MobileShopRamenLogFacade shops;

    private final ObjectMapper mapper;

    @Transactional
    public LegacyCopyReport copy() {
        Set<Long> members = accounts.copiedMemberIds();
        Set<Long> shopIds = shops.allShopIds();
        LegacyCopyTally logTally = new LegacyCopyTally("tb_v2_ramen_log");
        LegacyCopyTally imageTally = new LegacyCopyTally("tb_v2_ramen_log_image");
        LegacyCopyTally likeTally = new LegacyCopyTally("tb_v2_ramen_log_like");
        Map<Long, String> existingKeys = port.logIdempotencyKeys();
        Set<Long> copiedLogs = new HashSet<>();
        Set<Long> presentLogs = new HashSet<>();
        Set<Long> touchedUsers = new HashSet<>();
        Set<Long> touchedShops = new HashSet<>();
        for (LegacyRamenLog log : port.legacyLogs()) {
            LegacyCopyExclusion exclusion = null;
            if (existingKeys.containsKey(log.id())) {
                if (idempotencyKey(log.id()).equals(existingKeys.get(log.id()))) {
                    logTally.alreadyCopied();
                    if (hasImage(log)) {
                        imageTally.alreadyCopied();
                    }
                    else {
                        imageTally.exclude(LegacyCopyExclusion.NO_IMAGE);
                    }
                    presentLogs.add(log.id());
                    continue;
                }
                exclusion = LegacyCopyExclusion.ID_CONFLICT;
            }
            if (exclusion == null) {
                exclusion = exclusion(log, members, shopIds);
            }
            if (exclusion != null) {
                logTally.exclude(exclusion);
                imageTally.exclude(exclusion);
                continue;
            }
            port.insertLog(newLog(log));
            logTally.copied();
            copiedLogs.add(log.id());
            presentLogs.add(log.id());
            touchedUsers.add(log.memberId());
            touchedShops.add(log.shopId());
            if (hasImage(log)) {
                port.insertImage(log.id(), log.imageUrl());
                imageTally.copied();
            }
            else {
                imageTally.exclude(LegacyCopyExclusion.NO_IMAGE);
            }
        }
        Set<Long> likedLogs = copyLikes(members, presentLogs, likeTally);
        likedLogs.addAll(copiedLogs);
        port.recountLikes(likedLogs);
        accounts.updateLogCounts(withZeros(touchedUsers, port.liveLogCountsByUser(touchedUsers)));
        shops.updateLogCounts(withZeros(touchedShops, port.liveLogCountsByShop(touchedShops)));
        return LegacyCopyReport.of(logTally, imageTally, likeTally);
    }

    private Set<Long> copyLikes(Set<Long> members, Set<Long> presentLogs, LegacyCopyTally tally) {
        Set<LikeKey> existing = new HashSet<>(port.likeKeys());
        Set<Long> likedLogs = new HashSet<>();
        for (LegacyLike like : port.legacyLikes()) {
            LikeKey key = new LikeKey(like.memberId(), like.logId());
            if (existing.contains(key)) {
                tally.alreadyCopied();
            }
            else if (!presentLogs.contains(like.logId())) {
                tally.exclude(LegacyCopyExclusion.LOG_NOT_COPIED);
            }
            else if (!members.contains(like.memberId())) {
                tally.exclude(LegacyCopyExclusion.MEMBER_NOT_COPIED);
            }
            else {
                port.insertLike(key, LegacyKstTime.toUtc(like.createdAt()));
                existing.add(key);
                likedLogs.add(like.logId());
                tally.copied();
            }
        }
        return likedLogs;
    }

    private LegacyCopyExclusion exclusion(LegacyRamenLog log, Set<Long> members, Set<Long> shopIds) {
        if (log.deleted()) {
            return LegacyCopyExclusion.DELETED_LOG;
        }
        if (!members.contains(log.memberId())) {
            return LegacyCopyExclusion.MEMBER_NOT_COPIED;
        }
        if (!shopIds.contains(log.shopId())) {
            return LegacyCopyExclusion.SHOP_NOT_COPIED;
        }
        if (revisit(log.revisit()) == null) {
            return LegacyCopyExclusion.UNKNOWN_REVISIT;
        }
        if (length(log.menuName()) > MENU_NAME_MAX_LENGTH || length(log.note()) > NOTE_MAX_LENGTH) {
            return LegacyCopyExclusion.TOO_LONG;
        }
        return null;
    }

    private NewRamenLog newLog(LegacyRamenLog log) {
        String ramenType = OTHER_RAMEN_TYPE;
        if (MobileRamenLogService.RAMEN_TYPES.contains(log.ramenType())) {
            ramenType = log.ramenType();
        }
        String note = "";
        if (log.note() != null) {
            note = log.note();
        }
        LogVisibility visibility = LogVisibility.PRIVATE;
        if (log.publicLog()) {
            visibility = LogVisibility.PUBLIC;
        }
        return new NewRamenLog(log.id(), log.memberId(), log.shopId(), log.visitedAt(), log.menuName(), ramenType,
                revisit(log.revisit()), note, tasteCodes(log), visibility, idempotencyKey(log.id()),
                LegacyKstTime.toUtc(log.createdAt()), LegacyKstTime.toUtc(log.updatedAt()));
    }

    private List<String> tasteCodes(LegacyRamenLog log) {
        Set<String> codes = new LinkedHashSet<>();
        for (String labels : List.of(nullToEmpty(log.brothNotes()), nullToEmpty(log.noodleNotes()),
                nullToEmpty(log.seasoningNotes()), nullToEmpty(log.toppingNotes()))) {
            addCodes(labels, codes);
        }
        return List.copyOf(codes);
    }

    /** 못 찾은 라벨은 버린다. */
    private void addCodes(String labels, Set<String> codes) {
        if (labels.isBlank()) {
            return;
        }
        JsonNode node = mapper.readTree(labels);
        for (JsonNode label : node.values()) {
            String code = TASTE_CODES_BY_LABEL.get(label.asString().trim());
            if (code != null) {
                codes.add(code);
            }
        }
    }

    private static boolean hasImage(LegacyRamenLog log) {
        return log.imageUrl() != null && !log.imageUrl().isBlank();
    }

    private static RevisitIntention revisit(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "DEFINITELY" -> RevisitIntention.OFTEN;
            case "SOMETIMES" -> RevisitIntention.SOMETIMES;
            case "ONCE_ENOUGH" -> RevisitIntention.ONCE_IS_ENOUGH;
            default -> null;
        };
    }

    private static Map<Long, Integer> withZeros(Collection<Long> ids, Map<Long, Integer> counts) {
        Map<Long, Integer> result = new HashMap<>();
        for (Long id : ids) {
            result.put(id, counts.getOrDefault(id, 0));
        }
        return result;
    }

    private static String idempotencyKey(Long logId) {
        return IDEMPOTENCY_KEY_PREFIX + logId;
    }

    private static int length(String value) {
        if (value == null) {
            return 0;
        }
        return value.codePointCount(0, value.length());
    }

    private static String nullToEmpty(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }

    private static Map<String, String> tasteCodesByLabel() {
        Map<String, String> result = new HashMap<>();
        for (MobileTasteNote note : MobileTasteNote.values()) {
            result.put(note.label(), note.name());
        }
        result.put("국물 흡착 좋아요", MobileTasteNote.NOODLE_04.name());
        return Map.copyOf(result);
    }

}
