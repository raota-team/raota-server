package com.raota.mobile.account.domain.repository;

import com.raota.mobile.account.domain.model.MobileUserStatus;
import com.raota.mobile.account.domain.model.MobileUser;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface MobileUserRepository extends JpaRepository<MobileUser, Long> {

    @Modifying
    @Query("update MobileUser user set user.logCount = user.logCount + 1 where user.id = :id")
    void incrementLogCount(Long id);

    @Modifying
    @Query("""
            update MobileUser user set user.logCount =
                case when user.logCount > 0 then user.logCount - 1 else 0 end
            where user.id = :id
            """)
    void decrementLogCount(Long id);

    boolean existsByNicknameNormalizedAndIdNot(String nicknameNormalized, Long id);

    /** 같은 회원의 온보딩 완료 요청을 직렬화해 동의를 중복 기록하지 않는다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from MobileUser user where user.id = :id")
    Optional<MobileUser> findByIdForUpdate(Long id);

    @Query("""
            select user.id as id, user.purgeScheduledAt as purgeScheduledAt from MobileUser user
            where user.status = :status and user.purgeScheduledAt <= :now
            order by user.purgeScheduledAt, user.id
            """)
    List<DueUser> findDueForPurge(MobileUserStatus status, Instant now, Pageable pageable);

    @Query("""
            select user.id as id, user.purgeScheduledAt as purgeScheduledAt from MobileUser user
            where user.status = :status and user.purgeScheduledAt <= :now
            and (user.purgeScheduledAt > :afterScheduledAt
                or (user.purgeScheduledAt = :afterScheduledAt and user.id > :afterId))
            order by user.purgeScheduledAt, user.id
            """)
    List<DueUser> findDueForPurgeAfter(MobileUserStatus status, Instant now, Instant afterScheduledAt, Long afterId,
            Pageable pageable);

    interface DueUser {

        Long getId();

        Instant getPurgeScheduledAt();

    }

}
