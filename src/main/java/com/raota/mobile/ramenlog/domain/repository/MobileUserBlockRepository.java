package com.raota.mobile.ramenlog.domain.repository;

import com.raota.mobile.ramenlog.domain.model.MobileUserBlock;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MobileUserBlockRepository extends JpaRepository<MobileUserBlock, MobileUserBlock.Key> {

    @Modifying
    @Query(value = """
            INSERT IGNORE INTO tb_v2_user_block (blocker_id, blocked_id, created_at)
            VALUES (:blockerId, :blockedId, :createdAt)
            """, nativeQuery = true)
    void insertIfAbsent(Long blockerId, Long blockedId, Instant createdAt);

    @Modifying
    @Query("delete from MobileUserBlock block where block.blockerId = :blockerId and block.blockedId = :blockedId")
    void deleteBlock(Long blockerId, Long blockedId);

    List<MobileUserBlock> findByBlockerIdOrderByCreatedAtDescBlockedIdDesc(Long blockerId);

    @Modifying
    @Query("delete from MobileUserBlock block where block.blockerId = :userId or block.blockedId = :userId")
    void deleteInvolving(Long userId);

}
