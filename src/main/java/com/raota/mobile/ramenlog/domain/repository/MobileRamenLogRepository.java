package com.raota.mobile.ramenlog.domain.repository;

import com.raota.mobile.ramenlog.domain.model.MobileRamenLog;
import jakarta.persistence.LockModeType;
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
            select log.shopId as shopId, count(log) as logs,
                coalesce(sum(log.satisfactionScore), 0) as scoreSum, count(log.satisfactionScore) as scoredLogs
            from MobileRamenLog log
            where log.userId = :userId and log.deletedAt is null
            group by log.shopId
            """)
    List<ShopLogTotals> sumLiveLogsByShop(Long userId);

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
