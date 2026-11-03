package com.raota.mobile.account.domain.repository;

import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.model.MobileUserStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MobileUserRepository extends JpaRepository<MobileUser, Long> {

    /** 관리자 목록. 인자가 null이면 그 조건을 보지 않는다. {@code keyword}는 닉네임·이메일 부분 일치(대소문자 무시)이고 {@code userId}는 정확히 일치한다. */
    @Query("""
            select user from MobileUser user
            where (:status is null or user.status = :status)
                and (:keyword is null
                     or lower(user.nickname) like lower(concat('%', :keyword, '%'))
                     or lower(user.email) like lower(concat('%', :keyword, '%'))
                     or (:userId is not null and user.id = :userId))
            """)
    Page<MobileUser> findForAdmin(String keyword, Long userId, MobileUserStatus status, Pageable pageable);

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
    List<DueUser> findDueForPurgeAfter(
            MobileUserStatus status, Instant now, Instant afterScheduledAt, Long afterId, Pageable pageable);

    interface DueUser {

        Long getId();

        Instant getPurgeScheduledAt();
    }
}
