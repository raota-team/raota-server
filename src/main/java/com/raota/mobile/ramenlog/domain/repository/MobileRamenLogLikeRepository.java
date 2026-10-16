package com.raota.mobile.ramenlog.domain.repository;

import com.raota.mobile.ramenlog.domain.model.MobileRamenLogLike;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MobileRamenLogLikeRepository extends JpaRepository<MobileRamenLogLike, MobileRamenLogLike.Key> {

    /** 이미 공감했으면 0, 새로 공감했으면 1을 반환한다. */
    @Modifying
    @Query(value = """
            INSERT IGNORE INTO tb_v2_ramen_log_like (user_id, ramen_log_id, created_at)
            VALUES (:userId, :logId, :createdAt)
            """, nativeQuery = true)
    int insertIfAbsent(Long userId, Long logId, Instant createdAt);

    @Modifying
    @Query("delete from MobileRamenLogLike liked where liked.userId = :userId and liked.ramenLogId = :logId")
    int deleteLike(Long userId, Long logId);

    boolean existsByUserIdAndRamenLogId(Long userId, Long logId);

    @Query("""
            select liked.ramenLogId from MobileRamenLogLike liked
            where liked.userId = :userId and liked.ramenLogId in :logIds
            """)
    List<Long> findLikedLogIds(Long userId, Collection<Long> logIds);

    @Modifying
    @Query("delete from MobileRamenLogLike liked where liked.userId = :userId")
    void deleteByLiker(Long userId);

    @Modifying
    @Query("""
            delete from MobileRamenLogLike liked
            where liked.ramenLogId in (select log.id from MobileRamenLog log where log.userId = :userId)
            """)
    void deleteOnLogsOf(Long userId);

}
