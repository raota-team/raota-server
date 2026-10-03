package com.raota.mobile.ramenlog.application.service;

import com.raota.mobile.account.application.facade.MobileAccountRamenLogFacade;
import com.raota.mobile.account.application.facade.MobileAccountRamenLogFacade.MobileAuthor;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import com.raota.mobile.ramenlog.application.command.MobileCreateRamenLogCommand;
import com.raota.mobile.ramenlog.application.command.MobileUpdateRamenLogCommand;
import com.raota.mobile.ramenlog.application.result.MobileRamenLogDetail;
import com.raota.mobile.ramenlog.domain.model.LogVisibility;
import com.raota.mobile.ramenlog.domain.model.MobileRamenLog;
import com.raota.mobile.ramenlog.domain.model.MobileRamenLogImage;
import com.raota.mobile.ramenlog.domain.model.MobileTasteNote;
import com.raota.mobile.ramenlog.domain.repository.MobileRamenLogImageRepository;
import com.raota.mobile.ramenlog.domain.repository.MobileRamenLogRepository;
import com.raota.mobile.shop.application.facade.MobileShopRamenLogFacade;
import com.raota.mobile.shop.application.facade.MobileShopRamenLogFacade.MobileShopRef;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** 작성·수정·삭제와 회원·매장 집계가 함께 커밋되도록 묶는다. */
@Service
public class MobileRamenLogService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private static final Set<String> RAMEN_TYPES = Set.of("쇼유", "돈코츠", "이에케", "시오", "미소", "츠케멘", "탄탄멘", "마제소바", "아부라소바",
            "기타");

    private final MobileRamenLogRepository logs;

    private final MobileRamenLogImageRepository images;

    private final MobileShopRamenLogFacade shops;

    private final MobileAccountRamenLogFacade accounts;

    private final MobileUploadedImagePolicy imagePolicy;

    private final Clock clock;

    private final TransactionTemplate createTransaction;

    public MobileRamenLogService(MobileRamenLogRepository logs, MobileRamenLogImageRepository images,
            MobileShopRamenLogFacade shops, MobileAccountRamenLogFacade accounts, MobileUploadedImagePolicy imagePolicy,
            Clock clock, PlatformTransactionManager transactionManager) {
        this.logs = logs;
        this.images = images;
        this.shops = shops;
        this.accounts = accounts;
        this.imagePolicy = imagePolicy;
        this.clock = clock;
        this.createTransaction = new TransactionTemplate(transactionManager);
        this.createTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public MobileRamenLogDetail create(Long userId, String key, MobileCreateRamenLogCommand command) {
        MobileRamenLog previous = logs.findByUserIdAndIdempotencyKey(userId, key).orElse(null);
        if (previous != null) {
            rejectDeletedReplay(previous);
            return ownDetail(previous.getId(), userId);
        }
        Long shopId = parseId(command.shopId());
        validateVisitedAt(command.visitedAt());
        validateRamenType(command.ramenType());
        validateTasteCodes(command.tasteNoteCodes());
        validateImageUrls(command.imageUrls());
        shops.findPublishedShopRef(shopId);
        Instant now = now();
        Long id;
        try {
            // UNIQUE 충돌은 이 트랜잭션만 되돌린 뒤 별도 읽기로 먼저 커밋된 기록을 조회한다.
            id = createTransaction.execute(status -> {
                MobileRamenLog log = logs.saveAndFlush(MobileRamenLog.create(userId, shopId, command.visitedAt(),
                        command.menuName(), command.ramenType(), command.scores(), command.revisitIntention(),
                        command.note(), command.tasteNoteCodes(), command.visibility(), key, now));
                saveImages(log.getId(), command.imageUrls());
                accounts.incrementLogCount(userId);
                shops.recordLogAdded(shopId, log.satisfaction());
                return log.getId();
            });
        }
        catch (DataIntegrityViolationException exception) {
            MobileRamenLog replayed = logs.findByUserIdAndIdempotencyKey(userId, key).orElseThrow(() -> exception);
            rejectDeletedReplay(replayed);
            id = replayed.getId();
        }
        return ownDetail(id, userId);
    }

    @Transactional(readOnly = true)
    public MobileRamenLogDetail detail(Long logId, Long viewerId) {
        MobileRamenLog log = existing(logId);
        MobileAuthor author = author(log.getUserId());
        if (log.getVisibility() == LogVisibility.PRIVATE && !log.getUserId().equals(viewerId)) {
            throw notFound();
        }
        if (!author.active()) {
            throw notFound();
        }
        return response(log, author, viewerId);
    }

    @Transactional
    public MobileRamenLogDetail update(Long logId, Long userId, MobileUpdateRamenLogCommand command) {
        MobileRamenLog log = existingForUpdate(logId);
        requireAuthor(log, userId);
        if (command.visitedAt() != null) {
            validateVisitedAt(command.visitedAt());
        }
        if (command.ramenType() != null) {
            validateRamenType(command.ramenType());
        }
        if (command.tasteNoteCodes() != null) {
            validateTasteCodes(command.tasteNoteCodes());
        }
        if (command.imageUrls() != null) {
            validateImageUrls(command.imageUrls());
        }
        Integer before = log.satisfaction();
        log.edit(command.toEdit(), now());
        shops.recordSatisfactionChanged(log.getShopId(), before, log.satisfaction());
        if (command.imageUrls() != null) {
            images.deleteByRamenLogId(logId);
            images.flush();
            saveImages(logId, command.imageUrls());
        }
        return ownDetail(logId, userId);
    }

    @Transactional
    public void delete(Long logId, Long userId) {
        MobileRamenLog log = existingForUpdate(logId);
        requireAuthor(log, userId);
        log.softDelete(now());
        accounts.decrementLogCount(userId);
        shops.recordLogRemoved(log.getShopId(), log.satisfaction());
    }

    /**
     * 삭제한 기록의 키로 다시 오면 삭제된 기록을 돌려줄 수도, 새로 만들 수도 없다. 키는 UNIQUE로 남아 있으므로 409로 알린다.
     */
    private void rejectDeletedReplay(MobileRamenLog log) {
        if (log.getDeletedAt() != null) {
            throw new MobileException(MobileErrorCode.CONFLICT, "이미 삭제된 기록의 요청입니다.");
        }
    }

    private MobileRamenLogDetail ownDetail(Long logId, Long userId) {
        MobileRamenLog log = logs.findById(logId).orElseThrow(this::notFound);
        return response(log, author(log.getUserId()), userId);
    }

    private MobileRamenLogDetail response(MobileRamenLog log, MobileAuthor author, Long viewerId) {
        MobileShopRef shop = shops.findShopRefs(List.of(log.getShopId())).get(log.getShopId());
        if (shop == null) {
            throw new MobileException(MobileErrorCode.RESOURCE_NOT_FOUND, "매장을 찾을 수 없습니다.");
        }
        List<String> imageUrls = images.findByRamenLogIdInOrderByRamenLogIdAscSortOrderAsc(List.of(log.getId()))
            .stream()
            .map(MobileRamenLogImage::getUrl)
            .toList();
        return MobileRamenLogViews.detail(log, author, shop, imageUrls, viewerId);
    }

    private MobileRamenLog existingForUpdate(Long id) {
        MobileRamenLog log = logs.findByIdForUpdate(id).orElseThrow(this::notFound);
        if (log.getDeletedAt() != null) {
            throw notFound();
        }
        return log;
    }

    private MobileRamenLog existing(Long id) {
        MobileRamenLog log = logs.findById(id).orElseThrow(this::notFound);
        if (log.getDeletedAt() != null) {
            throw notFound();
        }
        return log;
    }

    private MobileAuthor author(Long userId) {
        MobileAuthor author = accounts.authors(List.of(userId)).get(userId);
        if (author == null) {
            throw notFound();
        }
        return author;
    }

    private void requireAuthor(MobileRamenLog log, Long userId) {
        if (!log.getUserId().equals(userId)) {
            throw new MobileException(MobileErrorCode.FORBIDDEN, "작성자만 기록을 변경할 수 있습니다.");
        }
    }

    private void saveImages(Long logId, List<String> urls) {
        if (urls.isEmpty()) {
            return;
        }
        List<MobileRamenLogImage> ordered = new ArrayList<>(urls.size());
        for (int index = 0; index < urls.size(); index++) {
            ordered.add(MobileRamenLogImage.of(logId, urls.get(index), index));
        }
        images.saveAllAndFlush(ordered);
    }

    private void validateVisitedAt(LocalDate visitedAt) {
        if (visitedAt.isAfter(LocalDate.now(clock.withZone(SEOUL)))) {
            throw invalid();
        }
    }

    private void validateRamenType(String ramenType) {
        if (!RAMEN_TYPES.contains(ramenType)) {
            throw invalid();
        }
    }

    private void validateTasteCodes(List<String> codes) {
        Set<String> distinct = new HashSet<>();
        for (String code : codes) {
            if (!distinct.add(code) || !MobileTasteNote.isCode(code)) {
                throw invalid();
            }
        }
    }

    private void validateImageUrls(List<String> urls) {
        for (String url : urls) {
            if (!imagePolicy.isOwnImage(url)) {
                throw invalid();
            }
        }
    }

    private Long parseId(String value) {
        try {
            return Long.valueOf(value);
        }
        catch (NumberFormatException exception) {
            throw invalid();
        }
    }

    private Instant now() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }

    private MobileException invalid() {
        return new MobileException(MobileErrorCode.VALIDATION_ERROR, "라멘 기록 입력값을 확인해 주세요.");
    }

    private MobileException notFound() {
        return new MobileException(MobileErrorCode.RESOURCE_NOT_FOUND, "기록을 찾을 수 없습니다.");
    }

}
