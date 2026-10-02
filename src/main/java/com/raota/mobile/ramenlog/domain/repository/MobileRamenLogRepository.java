package com.raota.mobile.ramenlog.domain.repository;

import com.raota.mobile.ramenlog.domain.model.MobileRamenLog;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

}
