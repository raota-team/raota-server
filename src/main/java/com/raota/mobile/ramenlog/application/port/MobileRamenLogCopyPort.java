package com.raota.mobile.ramenlog.application.port;

import com.raota.mobile.ramenlog.domain.model.LogVisibility;
import com.raota.mobile.ramenlog.domain.model.RevisitIntention;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** v1 기록·공감 테이블을 읽고 ID를 유지해 v2 기록 테이블에 쓴다. 시각은 저장된 값 그대로 주고받는다. */
public interface MobileRamenLogCopyPort {

    /** 삭제된 기록을 포함한 v1 기록 전체를 ID 순으로 읽는다. */
    List<LegacyRamenLog> legacyLogs();

    /** v1 공감 전체를 ID 순으로 읽는다. */
    List<LegacyLike> legacyLikes();

    /** v2 기록 ID별 멱등키(없으면 null). */
    Map<Long, String> logIdempotencyKeys();

    List<LikeKey> likeKeys();

    void insertLog(NewRamenLog log);

    void insertImage(Long logId, String url);

    void insertLike(LikeKey key, LocalDateTime createdAt);

    /** 지워지지 않은 v2 기록 수를 회원별로 센다. 기록이 없는 회원은 결과에 없다. */
    Map<Long, Integer> liveLogCountsByUser(Collection<Long> userIds);

    /** 지워지지 않은 v2 기록 수를 매장별로 센다. 기록이 없는 매장은 결과에 없다. */
    Map<Long, Integer> liveLogCountsByShop(Collection<Long> shopIds);

    /** 기록의 공감 수를 v2 공감 행 수로 다시 맞춘다. */
    void recountLikes(Collection<Long> logIds);

    record LegacyRamenLog(Long id, Long shopId, Long memberId, String imageUrl, String note, String brothNotes,
            String noodleNotes, String seasoningNotes, String toppingNotes, String revisit, boolean publicLog,
            LocalDate visitedAt, LocalDateTime createdAt, LocalDateTime updatedAt, String menuName, String ramenType,
            boolean deleted) {
    }

    record LegacyLike(Long logId, Long memberId, LocalDateTime createdAt) {
    }

    record LikeKey(Long userId, Long logId) {
    }

    /** 시각은 UTC다. 점수는 v1에 없어 비운다. */
    record NewRamenLog(Long id, Long userId, Long shopId, LocalDate visitedAt, String menuName, String ramenType,
            RevisitIntention revisitIntention, String note, List<String> tasteNoteCodes, LogVisibility visibility,
            String idempotencyKey, LocalDateTime createdAt, LocalDateTime updatedAt) {
    }

}
