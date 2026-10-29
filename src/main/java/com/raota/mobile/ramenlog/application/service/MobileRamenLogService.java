package com.raota.mobile.ramenlog.application.service;

import com.raota.mobile.account.application.facade.MobileAccountRamenLogFacade;
import com.raota.mobile.account.application.facade.MobileAccountRamenLogFacade.MobileAuthor;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import com.raota.mobile.ramenlog.application.command.MobileCreateRamenLogCommand;
import com.raota.mobile.ramenlog.application.command.MobileUpdateRamenLogCommand;
import com.raota.mobile.ramenlog.application.result.MobileRamenLogDetailResult;
import com.raota.mobile.ramenlog.domain.model.MobileRamenLog;
import com.raota.mobile.ramenlog.domain.model.MobileRamenLogImage;
import com.raota.mobile.ramenlog.domain.model.MobileTasteNote;
import com.raota.mobile.ramenlog.domain.repository.MobileRamenLogImageRepository;
import com.raota.mobile.ramenlog.domain.repository.MobileRamenLogLikeRepository;
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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** 작성·수정·삭제와 회원·매장 집계가 함께 커밋되도록 묶는다. */
@Service
@RequiredArgsConstructor
public class MobileRamenLogService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    static final Set<String> RAMEN_TYPES =
            Set.of("쇼유", "돈코츠", "이에케", "시오", "미소", "토리파이탄", "니보시", "지로", "츠케멘", "탄탄멘", "마제소바", "아부라소바", "기타");

    private final MobileRamenLogRepository ramenLogRepository;

    private final MobileRamenLogImageRepository imageRepository;

    private final MobileRamenLogLikeRepository likeRepository;

    private final MobileRamenLogCommentService commentService;

    private final MobileShopRamenLogFacade shopFacade;

    private final MobileAccountRamenLogFacade accountFacade;

    private final MobileUploadedImagePolicy uploadedImagePolicy;

    private final Clock clock;

    private final PlatformTransactionManager transactionManager;

    public MobileRamenLogDetailResult create(Long userId, String key, MobileCreateRamenLogCommand command) {
        MobileRamenLog previous =
                ramenLogRepository.findByUserIdAndIdempotencyKey(userId, key).orElse(null);
        if (previous != null) {
            rejectDeletedReplay(previous);
            return ownDetail(previous.getId(), userId);
        }
        Long shopId = parseId(command.shopId());
        validateContent(command.visitedAt(), command.ramenType(), command.tasteNoteCodes());
        validateImageUrls(command.imageUrls(), Set.of());
        shopFacade.findPublishedShopRef(shopId);
        Instant now = now();
        Long id;
        try {
            // UNIQUE 충돌은 이 트랜잭션만 되돌린 뒤 별도 읽기로 먼저 커밋된 기록을 조회한다.
            id = requiresNewTransaction().execute(status -> {
                MobileRamenLog ramenLog = ramenLogRepository.saveAndFlush(
                        MobileRamenLog.create(userId, shopId, command.toDraft(), key, now));
                saveImages(ramenLog.getId(), command.imageUrls());
                accountFacade.incrementLogCount(userId);
                shopFacade.recordLogAdded(shopId, ramenLog.satisfaction());
                return ramenLog.getId();
            });
        } catch (DataIntegrityViolationException exception) {
            MobileRamenLog replayed = ramenLogRepository
                    .findByUserIdAndIdempotencyKey(userId, key)
                    .orElseThrow(() -> exception);
            rejectDeletedReplay(replayed);
            id = replayed.getId();
        }
        return ownDetail(id, userId);
    }

    @Transactional(readOnly = true)
    public MobileRamenLogDetailResult detail(Long logId, Long viewerId) {
        MobileRamenLog ramenLog = existing(logId);
        MobileAuthor author = author(ramenLog.getUserId());
        if (!ramenLog.visibleTo(viewerId) || !author.active()) {
            throw new MobileException(MobileErrorCode.RESOURCE_NOT_FOUND, "기록을 찾을 수 없습니다.");
        }
        return response(ramenLog, author, viewerId);
    }

    @Transactional
    public MobileRamenLogDetailResult update(Long logId, Long userId, MobileUpdateRamenLogCommand command) {
        MobileRamenLog ramenLog = existingForUpdate(logId);
        requireAuthor(ramenLog, userId);
        validateContent(command.visitedAt(), command.ramenType(), command.tasteNoteCodes());
        if (command.imageUrls() != null) {
            validateImageUrls(command.imageUrls(), attachedImageUrls(logId));
        }
        Integer before = ramenLog.satisfaction();
        ramenLog.edit(command.toEdit(), now());
        shopFacade.recordSatisfactionChanged(ramenLog.getShopId(), before, ramenLog.satisfaction());
        if (command.imageUrls() != null) {
            imageRepository.deleteByRamenLogId(logId);
            imageRepository.flush();
            saveImages(logId, command.imageUrls());
        }
        return ownDetail(logId, userId);
    }

    @Transactional
    public void delete(Long logId, Long userId) {
        MobileRamenLog ramenLog = existingForUpdate(logId);
        requireAuthor(ramenLog, userId);
        remove(ramenLog);
    }

    /** 관리자가 신고된 기록을 지운다. 이미 지워진 기록이면 아무것도 하지 않는다. 작성자가 지울 때와 같이 회원·매장 집계를 줄인다. */
    @Transactional
    public void removeByModerator(Long logId) {
        MobileRamenLog ramenLog = ramenLogRepository
                .findByIdForUpdate(logId)
                .orElseThrow(() -> new MobileException(MobileErrorCode.RESOURCE_NOT_FOUND, "기록을 찾을 수 없습니다."));
        if (ramenLog.getDeletedAt() == null) {
            remove(ramenLog);
        }
    }

    private void remove(MobileRamenLog ramenLog) {
        ramenLog.softDelete(now());
        accountFacade.decrementLogCount(ramenLog.getUserId());
        shopFacade.recordLogRemoved(ramenLog.getShopId(), ramenLog.satisfaction());
    }

    /**
     * 삭제한 기록의 키로 다시 오면 삭제된 기록을 돌려줄 수도, 새로 만들 수도 없다. 키는 UNIQUE로 남아 있으므로 409로 알린다.
     */
    private void rejectDeletedReplay(MobileRamenLog ramenLog) {
        if (ramenLog.getDeletedAt() != null) {
            throw new MobileException(MobileErrorCode.CONFLICT, "이미 삭제된 기록의 요청입니다.");
        }
    }

    private MobileRamenLogDetailResult ownDetail(Long logId, Long userId) {
        MobileRamenLog ramenLog = ramenLogRepository
                .findById(logId)
                .orElseThrow(() -> new MobileException(MobileErrorCode.RESOURCE_NOT_FOUND, "기록을 찾을 수 없습니다."));
        return response(ramenLog, author(ramenLog.getUserId()), userId);
    }

    private MobileRamenLogDetailResult response(MobileRamenLog ramenLog, MobileAuthor author, Long viewerId) {
        MobileShopRef shop =
                shopFacade.findShopRefs(List.of(ramenLog.getShopId())).get(ramenLog.getShopId());
        if (shop == null) {
            throw new MobileException(MobileErrorCode.RESOURCE_NOT_FOUND, "매장을 찾을 수 없습니다.");
        }
        List<String> imageUrls =
                imageRepository.findByRamenLogIdInOrderByRamenLogIdAscSortOrderAsc(List.of(ramenLog.getId())).stream()
                        .map(MobileRamenLogImage::getUrl)
                        .toList();
        boolean liked = viewerId != null && likeRepository.existsByUserIdAndRamenLogId(viewerId, ramenLog.getId());
        return MobileRamenLogResultAssembler.toDetail(
                ramenLog, author, shop, imageUrls, liked, commentService.preview(ramenLog.getId(), viewerId), viewerId);
    }

    private MobileRamenLog existingForUpdate(Long id) {
        MobileRamenLog ramenLog = ramenLogRepository
                .findByIdForUpdate(id)
                .orElseThrow(() -> new MobileException(MobileErrorCode.RESOURCE_NOT_FOUND, "기록을 찾을 수 없습니다."));
        if (ramenLog.getDeletedAt() != null) {
            throw new MobileException(MobileErrorCode.RESOURCE_NOT_FOUND, "기록을 찾을 수 없습니다.");
        }
        return ramenLog;
    }

    private MobileRamenLog existing(Long id) {
        MobileRamenLog ramenLog = ramenLogRepository
                .findById(id)
                .orElseThrow(() -> new MobileException(MobileErrorCode.RESOURCE_NOT_FOUND, "기록을 찾을 수 없습니다."));
        if (ramenLog.getDeletedAt() != null) {
            throw new MobileException(MobileErrorCode.RESOURCE_NOT_FOUND, "기록을 찾을 수 없습니다.");
        }
        return ramenLog;
    }

    private MobileAuthor author(Long userId) {
        MobileAuthor author = accountFacade.authors(List.of(userId)).get(userId);
        if (author == null) {
            throw new MobileException(MobileErrorCode.RESOURCE_NOT_FOUND, "기록을 찾을 수 없습니다.");
        }
        return author;
    }

    private void requireAuthor(MobileRamenLog ramenLog, Long userId) {
        if (!ramenLog.getUserId().equals(userId)) {
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
        imageRepository.saveAllAndFlush(ordered);
    }

    /** 작성은 모든 항목을, 부분 수정은 보낸 항목(null이 아닌 것)만 검사한다. */
    private void validateContent(LocalDate visitedAt, String ramenType, List<String> tasteNoteCodes) {
        boolean futureVisit = visitedAt != null && visitedAt.isAfter(LocalDate.now(clock.withZone(SEOUL)));
        boolean unknownRamenType = ramenType != null && !RAMEN_TYPES.contains(ramenType);
        boolean invalidTasteCodes = tasteNoteCodes != null && !isValidTasteCodes(tasteNoteCodes);
        if (futureVisit || unknownRamenType || invalidTasteCodes) {
            throw new MobileException(MobileErrorCode.VALIDATION_ERROR, "라멘 기록 입력값을 확인해 주세요.");
        }
    }

    private static boolean isValidTasteCodes(List<String> codes) {
        return new HashSet<>(codes).size() == codes.size() && codes.stream().allMatch(MobileTasteNote::isCode);
    }

    /**
     * 같은 주소를 두 번 붙일 수 없고, 새로 붙이는 주소는 앱이 발급한 기록 사진 주소여야 한다. 이미 붙어 있던 사진({@code attached}, 웹에서
     * 복사한 기록의 외부 주소 포함)은 그대로 둘 수 있다.
     */
    private void validateImageUrls(List<String> urls, Set<String> attached) {
        boolean duplicated = new HashSet<>(urls).size() != urls.size();
        boolean notUploaded =
                urls.stream().anyMatch(url -> !attached.contains(url) && !uploadedImagePolicy.isRamenLogImage(url));
        if (duplicated || notUploaded) {
            throw new MobileException(MobileErrorCode.VALIDATION_ERROR, "라멘 기록 입력값을 확인해 주세요.");
        }
    }

    private Set<String> attachedImageUrls(Long logId) {
        return imageRepository.findByRamenLogIdInOrderByRamenLogIdAscSortOrderAsc(List.of(logId)).stream()
                .map(MobileRamenLogImage::getUrl)
                .collect(Collectors.toSet());
    }

    private Long parseId(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new MobileException(MobileErrorCode.VALIDATION_ERROR, "라멘 기록 입력값을 확인해 주세요.");
        }
    }

    private Instant now() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }

    /** 작성을 바깥과 별도 트랜잭션으로 커밋한다. 멱등키 UNIQUE 충돌이 나도 이 트랜잭션만 되돌리고 먼저 커밋된 기록을 다시 읽을 수 있다. */
    private TransactionTemplate requiresNewTransaction() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }
}
