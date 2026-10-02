package com.raota.mobile.ramenlog.domain.repository;

import com.raota.mobile.ramenlog.domain.model.MobileRamenLog;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface MobileRamenLogRepository extends JpaRepository<MobileRamenLog, Long> {

    Optional<MobileRamenLog> findByUserIdAndIdempotencyKey(Long userId, String key);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select log from MobileRamenLog log where log.id = :id")
    Optional<MobileRamenLog> findByIdForUpdate(Long id);

}
