package com.raota.ramenlog.application;

import com.raota.account.domain.member.model.MemberProfile;
import com.raota.account.domain.member.repository.MemberRepository;
import com.raota.account.application.member.MemberActivityVisibilityService;
import com.raota.ramenshop.domain.model.RamenShop;
import com.raota.ramenshop.domain.repository.RamenShopRepository;
import com.raota.ramenlog.domain.model.RamenLog;
import com.raota.ramenlog.domain.model.RamenLogLike;
import com.raota.ramenlog.domain.repository.RamenLogLikeRepository;
import com.raota.ramenlog.domain.repository.RamenLogRepository;
import com.raota.global.cache.CacheInvalidationPublisher;
import com.raota.global.file.FileUploader;
import com.raota.ramenlog.presentation.api.request.RamenLogSort;
import com.raota.ramenlog.presentation.api.request.RamenLogUpsertRequest;
import com.raota.ramenlog.presentation.api.response.RamenLogLikeResponse;
import com.raota.ramenlog.presentation.api.response.RamenLogResponse;
import com.raota.ramenlog.presentation.api.response.RamenLogShopResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RamenLogService {

    private final RamenLogRepository ramenLogRepository;
    private final RamenLogLikeRepository ramenLogLikeRepository;
    private final MemberRepository memberRepository;
    private final RamenShopRepository ramenShopRepository;
    private final FileUploader fileUploader;
    private final CacheInvalidationPublisher cacheInvalidationPublisher;
    private final MemberActivityVisibilityService memberActivityVisibilityService;

    public Page<RamenLogResponse> getPublicLogs(
            Long viewerId,
            RamenLogSort sort,
            Long shopId,
            String keyword,
            Pageable pageable
    ) {
        Specification<RamenLog> specification = filter(true, null, shopId, keyword);
        return ramenLogRepository.findAll(specification, pageRequest(pageable, sort))
                .map(log -> toResponse(log, viewerId));
    }

    public RamenLogResponse getLog(Long logId, Long viewerId) {
        RamenLog log = getActiveLog(logId);
        memberActivityVisibilityService.requireLogsVisible(log.getAuthor().getId(), viewerId);
        if (!log.isPublic() && !Objects.equals(log.getAuthor().getId(), viewerId)) {
            throw new EntityNotFoundException("라멘로그를 찾을 수 없습니다.");
        }
        return toResponse(log, viewerId);
    }

    public Page<RamenLogResponse> getMemberLogs(
            Long targetMemberId,
            Long viewerId,
            boolean includePrivate,
            Long shopId,
            Pageable pageable
    ) {
        memberActivityVisibilityService.requireLogsVisible(targetMemberId, viewerId);
        requireMember(targetMemberId);
        Specification<RamenLog> specification = filter(
                includePrivate ? null : true,
                targetMemberId,
                shopId,
                null
        );
        return ramenLogRepository.findAll(specification, pageRequest(pageable, RamenLogSort.LATEST))
                .map(log -> toResponse(log, viewerId));
    }

    public List<RamenLogShopResponse> getMemberLogShops(
            Long targetMemberId,
            Long viewerId,
            boolean includePrivate
    ) {
        memberActivityVisibilityService.requireLogsVisible(targetMemberId, viewerId);
        requireMember(targetMemberId);
        List<RamenLog> logs = ramenLogRepository.findAll(filter(
                includePrivate ? null : true,
                targetMemberId,
                null,
                null
        ));
        return logs.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        RamenLog::getRamenShop,
                        java.util.stream.Collectors.counting()
                ))
                .entrySet().stream()
                .map(entry -> new RamenLogShopResponse(
                        entry.getKey().getId(),
                        entry.getKey().getName(),
                        entry.getValue()
                ))
                .sorted(java.util.Comparator.comparing(RamenLogShopResponse::name))
                .toList();
    }

    @Transactional
    public RamenLogResponse create(RamenLogUpsertRequest request, Long memberId) {
        MemberProfile member = requireMember(memberId);
        RamenShop shop = requireShop(request.shopId());
        RamenLogUpsertRequest.TasteNotesRequest notes = request.normalizedTasteNotes();
        boolean firstLogAtShop = ramenLogRepository
                .countByAuthorIdAndRamenShopIdAndIsDeletedFalse(memberId, shop.getId()) == 0;

        RamenLog log = RamenLog.builder()
                .author(member)
                .ramenShop(shop)
                .menuName(request.menuName().trim())
                .ramenType(request.ramenType().trim())
                .imageUrl(request.imageUrl().trim())
                .visitedAt(request.visitedAt())
                .note(normalizeNote(request.note()))
                .brothNotes(safe(notes.broth()))
                .noodleNotes(safe(notes.noodle()))
                .seasoningNotes(safe(notes.seasoning()))
                .toppingNotes(safe(notes.topping()))
                .revisit(request.revisit())
                .isPublic(request.isPublic())
                .build();

        RamenLog saved = ramenLogRepository.save(log);
        if (firstLogAtShop) {
            member.increaseVisitedRestaurantCount();
        }
        member.increasePhotoCount();
        shop.increaseVisitCount();
        invalidateShop(shop.getId());
        return toResponse(saved, memberId);
    }

    @Transactional
    public RamenLogResponse update(Long logId, RamenLogUpsertRequest request, Long memberId) {
        RamenLog log = getOwnedLog(logId, memberId);
        RamenShop previousShop = log.getRamenShop();
        RamenShop shop = requireShop(request.shopId());
        RamenLogUpsertRequest.TasteNotesRequest notes = request.normalizedTasteNotes();
        boolean shopChanged = !Objects.equals(previousShop.getId(), shop.getId());
        MemberProfile member = log.getAuthor();

        if (shopChanged) {
            long previousShopLogs = ramenLogRepository
                    .countByAuthorIdAndRamenShopIdAndIsDeletedFalse(memberId, previousShop.getId());
            long nextShopLogs = ramenLogRepository
                    .countByAuthorIdAndRamenShopIdAndIsDeletedFalse(memberId, shop.getId());
            if (previousShopLogs == 1) {
                member.decreaseVisitedRestaurantCount();
            }
            if (nextShopLogs == 0) {
                member.increaseVisitedRestaurantCount();
            }
            previousShop.decreaseVisitCount();
            shop.increaseVisitCount();
        }

        log.update(
                shop,
                request.menuName().trim(),
                request.ramenType().trim(),
                request.imageUrl().trim(),
                normalizeNote(request.note()),
                safe(notes.broth()),
                safe(notes.noodle()),
                safe(notes.seasoning()),
                safe(notes.topping()),
                request.revisit(),
                request.visitedAt(),
                request.isPublic()
        );
        if (shopChanged) {
            invalidateShop(previousShop.getId());
            invalidateShop(shop.getId());
        }
        return toResponse(log, memberId);
    }

    @Transactional
    public void delete(Long logId, Long memberId) {
        RamenLog log = getOwnedLog(logId, memberId);
        MemberProfile member = log.getAuthor();
        RamenShop shop = log.getRamenShop();
        long currentShopLogs = ramenLogRepository
                .countByAuthorIdAndRamenShopIdAndIsDeletedFalse(memberId, shop.getId());
        ramenLogLikeRepository.deleteAllByRamenLogId(logId);
        log.delete();
        if (currentShopLogs == 1) {
            member.decreaseVisitedRestaurantCount();
        }
        member.decreasePhotoCount();
        shop.decreaseVisitCount();
        invalidateShop(shop.getId());
    }

    @Transactional
    public RamenLogLikeResponse toggleLike(Long logId, Long memberId) {
        RamenLog log = getActiveLog(logId);
        MemberProfile member = requireMember(memberId);
        if (!log.isPublic() && !Objects.equals(log.getAuthor().getId(), memberId)) {
            throw new EntityNotFoundException("라멘로그를 찾을 수 없습니다.");
        }

        return ramenLogLikeRepository.findByRamenLogIdAndMemberId(logId, memberId)
                .map(like -> {
                    ramenLogLikeRepository.delete(like);
                    log.decreaseLikeCount();
                    return new RamenLogLikeResponse(false, log.getLikeCount());
                })
                .orElseGet(() -> {
                    ramenLogLikeRepository.save(RamenLogLike.builder()
                            .ramenLog(log)
                            .member(member)
                            .build());
                    log.increaseLikeCount();
                    return new RamenLogLikeResponse(true, log.getLikeCount());
                });
    }

    private RamenLogResponse toResponse(RamenLog log, Long viewerId) {
        boolean liked = viewerId != null
                && ramenLogLikeRepository.existsByRamenLogIdAndMemberId(log.getId(), viewerId);
        return RamenLogResponse.from(log, viewerId, liked, fileUploader);
    }

    private Specification<RamenLog> filter(Boolean isPublic, Long memberId, Long shopId, String keyword) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isFalse(root.get("isDeleted")));

            if (isPublic != null) {
                predicates.add(criteriaBuilder.equal(root.get("isPublic"), isPublic));
                if (isPublic) {
                    predicates.add(criteriaBuilder.isTrue(
                            root.get("author").get("activityVisibility").get("logsPublic")
                    ));
                }
            }
            if (memberId != null) {
                predicates.add(criteriaBuilder.equal(root.get("author").get("id"), memberId));
            }
            if (shopId != null) {
                predicates.add(criteriaBuilder.equal(root.get("ramenShop").get("id"), shopId));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("ramenShop").get("name")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("menuName")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("ramenType")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("note")), pattern)
                ));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Pageable pageRequest(Pageable pageable, RamenLogSort sort) {
        Sort resolvedSort = sort == RamenLogSort.POPULAR
                ? Sort.by(Sort.Order.desc("likeCount"), Sort.Order.desc("visitedAt"), Sort.Order.desc("createdAt"))
                : Sort.by(Sort.Order.desc("visitedAt"), Sort.Order.desc("createdAt"));
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), resolvedSort);
    }

    private RamenLog getActiveLog(Long logId) {
        return ramenLogRepository.findByIdAndIsDeletedFalse(logId)
                .orElseThrow(() -> new EntityNotFoundException("라멘로그를 찾을 수 없습니다."));
    }

    private RamenLog getOwnedLog(Long logId, Long memberId) {
        RamenLog log = getActiveLog(logId);
        if (!Objects.equals(log.getAuthor().getId(), memberId)) {
            throw new AccessDeniedException("본인의 라멘로그만 수정하거나 삭제할 수 있습니다.");
        }
        return log;
    }

    private MemberProfile requireMember(Long memberId) {
        return memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
    }

    private RamenShop requireShop(Long shopId) {
        return ramenShopRepository.findById(shopId)
                .orElseThrow(() -> new EntityNotFoundException("라멘집을 찾을 수 없습니다."));
    }

    private static List<String> safe(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private static String normalizeNote(String note) {
        return note == null || note.isBlank() ? null : note.trim();
    }

    private void invalidateShop(Long shopId) {
        cacheInvalidationPublisher.publish("ramenShopDetail", String.valueOf(shopId));
        cacheInvalidationPublisher.publishAll("ramenShopList");
    }
}
