package com.raota.mobile.notification.domain.repository;

import com.raota.mobile.notification.domain.model.MobilePushToken;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MobilePushTokenRepository extends JpaRepository<MobilePushToken, Long> {

    /** 다른 회원이 같은 기기로 로그인하면 토큰이 새 회원에게 옮겨 가, 이전 회원의 알림이 그 기기로 가지 않는다. */
    @Modifying
    @Query(value = """
            INSERT INTO tb_v2_push_token (user_id, token, platform, language, created_at, updated_at)
            VALUES (:userId, :token, :platform, :language, :now, :now)
            ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), platform = VALUES(platform),
                language = VALUES(language), updated_at = VALUES(updated_at)
            """, nativeQuery = true)
    void upsert(Long userId, String token, String platform, String language, Instant now);

    List<MobilePushToken> findByUserIdIn(List<Long> userIds);

    @Modifying
    @Query("delete from MobilePushToken token where token.userId = :userId and token.token = :token")
    int deleteOwned(Long userId, String token);

    @Modifying
    @Query("delete from MobilePushToken token where token.token in :tokens")
    int deleteByTokenIn(Set<String> tokens);

    @Modifying
    @Query("delete from MobilePushToken token where token.userId = :userId")
    void deleteForUser(Long userId);
}
