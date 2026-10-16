package com.raota.mobile.ramenlog.domain.repository;

import com.raota.mobile.ramenlog.domain.model.LogVisibility;
import com.raota.mobile.ramenlog.domain.model.MobileRamenLog;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MobileRamenLogRepository extends JpaRepository<MobileRamenLog, Long> {

    Optional<MobileRamenLog> findByUserIdAndIdempotencyKey(Long userId, String key);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select log from MobileRamenLog log where log.id = :id")
    Optional<MobileRamenLog> findByIdForUpdate(Long id);

    @Query("""
            select log from MobileRamenLog log
            where log.userId = :userId and log.deletedAt is null
                and (:afterDate is null or log.visitedAt < :afterDate
                     or (log.visitedAt = :afterDate and log.id < :afterId))
            order by log.visitedAt desc, log.id desc
            """)
    List<MobileRamenLog> findMine(Long userId, LocalDate afterDate, Long afterId, Pageable limit);

    @Query("""
            select log from MobileRamenLog log
            where log.visibility = :visibility and log.deletedAt is null
                and (:ramenType is null or log.ramenType = :ramenType)
                and (:shopId is null or log.shopId = :shopId)
                and (:afterCreatedAt is null or log.createdAt < :afterCreatedAt
                     or (log.createdAt = :afterCreatedAt and log.id < :afterId))
            order by log.createdAt desc, log.id desc
            """)
    List<MobileRamenLog> findLatest(LogVisibility visibility, String ramenType, Long shopId, Instant afterCreatedAt,
            Long afterId, Pageable limit);

    @Query("""
            select log from MobileRamenLog log
            where log.visibility = :visibility and log.deletedAt is null
                and (:ramenType is null or log.ramenType = :ramenType)
                and (:shopId is null or log.shopId = :shopId)
                and (:afterLikes is null or log.likeCount < :afterLikes
                     or (log.likeCount = :afterLikes and log.id < :afterId))
            order by log.likeCount desc, log.id desc
            """)
    List<MobileRamenLog> findMostLiked(LogVisibility visibility, String ramenType, Long shopId, Integer afterLikes,
            Long afterId, Pageable limit);

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

}
