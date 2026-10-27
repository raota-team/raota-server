package com.raota.mobile.ramenlog.domain.repository;

import com.raota.mobile.ramenlog.domain.model.MobileRamenLogComment;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MobileRamenLogCommentRepository extends JpaRepository<MobileRamenLogComment, Long> {

    Optional<MobileRamenLogComment> findByUserIdAndIdempotencyKey(Long userId, String key);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select comment from MobileRamenLogComment comment where comment.id = :id")
    Optional<MobileRamenLogComment> findByIdForUpdate(Long id);

    /** 잠그기 전에 엔티티를 영속성 컨텍스트에 올리지 않으려고 부모 기록 ID만 읽는다. */
    @Query("select comment.ramenLogId from MobileRamenLogComment comment where comment.id = :id")
    Optional<Long> findRamenLogIdById(Long id);

    /** 로그인한 회원에게는 그 회원이 차단한 작성자의 댓글과 그 회원이 신고한 댓글을 빼고 보여준다. */
    @Query("""
            select comment from MobileRamenLogComment comment
            where comment.ramenLogId = :logId and comment.deletedAt is null
                and (:viewerId is null or (
                    comment.userId not in (
                        select block.blockedId from MobileUserBlock block where block.blockerId = :viewerId)
                    and comment.id not in (
                        select report.targetId from MobileContentReport report
                        where report.reporterId = :viewerId
                            and report.targetType
                                = com.raota.mobile.ramenlog.domain.model.ReportTargetType.RAMEN_LOG_COMMENT)))
                and (:afterCreatedAt is null or comment.createdAt > :afterCreatedAt
                     or (comment.createdAt = :afterCreatedAt and comment.id > :afterId))
            order by comment.createdAt asc, comment.id asc
            """)
    List<MobileRamenLogComment> findPage(
            Long logId, Long viewerId, Instant afterCreatedAt, Long afterId, Pageable limit);

    @Query("""
            select comment.ramenLogId as logId, count(comment) as comments
            from MobileRamenLogComment comment
            where comment.userId = :userId and comment.deletedAt is null
            group by comment.ramenLogId
            """)
    List<LogCommentTotals> countLiveByLog(Long userId);

    @Modifying
    @Query("delete from MobileRamenLogComment comment where comment.userId = :userId")
    void deleteByWriter(Long userId);

    @Modifying
    @Query("""
            delete from MobileRamenLogComment comment
            where comment.ramenLogId in (select log.id from MobileRamenLog log where log.userId = :userId)
            """)
    void deleteOnLogsOf(Long userId);

    /** 기록별 삭제되지 않은 댓글 수다. */
    interface LogCommentTotals {

        Long getLogId();

        Long getComments();
    }
}
