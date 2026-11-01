package com.raota.mobile.ramenlog.domain.repository;

import com.raota.mobile.ramenlog.domain.model.LogVisibility;
import com.raota.mobile.ramenlog.domain.model.MobileRamenLog;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MobileRamenLogRepository extends JpaRepository<MobileRamenLog, Long> {

    Optional<MobileRamenLog> findByUserIdAndIdempotencyKey(Long userId, String key);

    /** 이 회원들이 {@code [from, to)} 사이에 쓰고 지우지 않은 기록이다. */
    @Query("""
            select log from MobileRamenLog log
            where log.userId in :userIds and log.deletedAt is null and log.createdAt >= :from and log.createdAt < :to
            """)
    List<MobileRamenLog> findLiveCreatedBetween(Collection<Long> userIds, Instant from, Instant to);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select log from MobileRamenLog log where log.id = :id")
    Optional<MobileRamenLog> findByIdForUpdate(Long id);

    /**
     * 관리자 목록. 삭제된 기록은 빼고, 인자가 null이면 그 조건을 보지 않는다. {@code keyword}는 메뉴명·메모 부분 일치다. 비공개 기록의 내용이 검색으로
     * 드러나지 않게 {@code keyword}나 {@code shopId}가 있으면 공개 기록만 찾는다.
     */
    @Query("""
            select log from MobileRamenLog log
            where log.deletedAt is null
                and (:visibility is null or log.visibility = :visibility)
                and ((:keyword is null and :shopId is null)
                     or log.visibility = com.raota.mobile.ramenlog.domain.model.LogVisibility.PUBLIC)
                and (:keyword is null or log.menuName like concat('%', :keyword, '%')
                     or log.note like concat('%', :keyword, '%'))
                and (:userId is null or log.userId = :userId)
                and (:shopId is null or log.shopId = :shopId)
            """)
    Page<MobileRamenLog> findForAdmin(
            LogVisibility visibility, String keyword, Long userId, Long shopId, Pageable pageable);

    @Query("""
            select coalesce(sum(case when log.visibility = com.raota.mobile.ramenlog.domain.model.LogVisibility.PUBLIC
                    then 1 else 0 end), 0) as publicCount,
                coalesce(sum(case when log.visibility = com.raota.mobile.ramenlog.domain.model.LogVisibility.PRIVATE
                    then 1 else 0 end), 0) as privateCount
            from MobileRamenLog log
            where log.userId = :userId and log.deletedAt is null
            """)
    VisibilityCounts countLiveByVisibility(Long userId);

    /** {@code ramenType}이 null이면 종류를 보지 않는다. 비공개 기록도 포함한다. */
    @Query("""
            select log from MobileRamenLog log
            where log.userId = :userId and log.deletedAt is null
                and (:ramenType is null or log.ramenType = :ramenType)
                and (:afterDate is null or log.visitedAt < :afterDate
                     or (log.visitedAt = :afterDate and log.id < :afterId))
            order by log.visitedAt desc, log.id desc
            """)
    List<MobileRamenLog> findMine(Long userId, String ramenType, LocalDate afterDate, Long afterId, Pageable limit);

    /**
     * 로그인한 회원에게는 그 회원이 차단한 작성자의 기록과 그 회원이 신고한 기록을 빼고 보여준다. {@code authorId}가 null이 아니면 그 회원의 기록만이다.
     */
    @Query("""
            select log from MobileRamenLog log
            where log.visibility = :visibility and log.deletedAt is null
                and (:ramenType is null or log.ramenType = :ramenType)
                and (:shopId is null or log.shopId = :shopId)
                and (:authorId is null or log.userId = :authorId)
                and (:viewerId is null or (
                    log.userId not in (
                        select block.blockedId from MobileUserBlock block where block.blockerId = :viewerId)
                    and log.id not in (
                        select report.targetId from MobileContentReport report
                        where report.reporterId = :viewerId
                            and report.targetType = com.raota.mobile.ramenlog.domain.model.ReportTargetType.RAMEN_LOG)))
                and (:afterCreatedAt is null or log.createdAt < :afterCreatedAt
                     or (log.createdAt = :afterCreatedAt and log.id < :afterId))
            order by log.createdAt desc, log.id desc
            """)
    List<MobileRamenLog> findLatest(
            LogVisibility visibility,
            String ramenType,
            Long shopId,
            Long authorId,
            Long viewerId,
            Instant afterCreatedAt,
            Long afterId,
            Pageable limit);

    /** {@link #findLatest}와 같은 조건을 공감 수 순으로 읽는다. */
    @Query("""
            select log from MobileRamenLog log
            where log.visibility = :visibility and log.deletedAt is null
                and (:ramenType is null or log.ramenType = :ramenType)
                and (:shopId is null or log.shopId = :shopId)
                and (:authorId is null or log.userId = :authorId)
                and (:viewerId is null or (
                    log.userId not in (
                        select block.blockedId from MobileUserBlock block where block.blockerId = :viewerId)
                    and log.id not in (
                        select report.targetId from MobileContentReport report
                        where report.reporterId = :viewerId
                            and report.targetType = com.raota.mobile.ramenlog.domain.model.ReportTargetType.RAMEN_LOG)))
                and (:afterLikes is null or log.likeCount < :afterLikes
                     or (log.likeCount = :afterLikes and log.id < :afterId))
            order by log.likeCount desc, log.id desc
            """)
    List<MobileRamenLog> findMostLiked(
            LogVisibility visibility,
            String ramenType,
            Long shopId,
            Long authorId,
            Long viewerId,
            Integer afterLikes,
            Long afterId,
            Pageable limit);

    @Query("""
            select log.shopId as shopId, count(log) as logs,
                coalesce(sum(log.satisfactionScore), 0) as scoreSum, count(log.satisfactionScore) as scoredLogs
            from MobileRamenLog log
            where log.userId = :userId and log.deletedAt is null
            group by log.shopId
            """)
    List<ShopLogTotals> sumLiveLogsByShop(Long userId);

    /** 회원이 공감했던 기록의 공감 수를 하나씩 줄인다. 공감 행을 지우기 전에 호출한다. */
    @Modifying
    @Query("""
            update MobileRamenLog log set log.likeCount = log.likeCount - 1
            where log.likeCount > 0
                and log.id in (select liked.ramenLogId from MobileRamenLogLike liked where liked.userId = :userId)
            """)
    void decrementLikesBy(Long userId);

    /** 탈퇴 정리에서 한 기록의 댓글 수를 한 번에 줄인다. 0 밑으로 내려가지 않는다. */
    @Modifying
    @Query("""
            update MobileRamenLog log
            set log.commentCount = case when log.commentCount > :count then log.commentCount - :count else 0 end
            where log.id = :logId
            """)
    void removeComments(Long logId, int count);

    @Modifying
    @Query("delete from MobileRamenLog log where log.userId = :userId")
    void deleteForUser(Long userId);

    /** 매장별 삭제되지 않은 기록 수와 만족도 합계다. */
    interface ShopLogTotals {

        Long getShopId();

        Long getLogs();

        Long getScoreSum();

        Long getScoredLogs();
    }

    /** 한 회원의 삭제되지 않은 공개·비공개 기록 수다. */
    interface VisibilityCounts {

        Long getPublicCount();

        Long getPrivateCount();
    }
}
