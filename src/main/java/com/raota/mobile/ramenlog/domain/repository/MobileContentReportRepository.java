package com.raota.mobile.ramenlog.domain.repository;

import com.raota.mobile.ramenlog.domain.model.MobileContentReport;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MobileContentReportRepository extends JpaRepository<MobileContentReport, Long> {

    /** 같은 대상을 이미 신고했으면 0, 새로 접수했으면 1을 반환한다. */
    @Modifying
    @Query(value = """
            INSERT IGNORE INTO tb_v2_content_report (reporter_id, target_type, target_id, reason, status, created_at)
            VALUES (:reporterId, :targetType, :targetId, :reason, 'OPEN', :createdAt)
            """, nativeQuery = true)
    int insertIfAbsent(Long reporterId, String targetType, Long targetId, String reason, Instant createdAt);

    @Modifying
    @Query("delete from MobileContentReport report where report.reporterId = :userId")
    void deleteByReporter(Long userId);

    /** 회원의 기록, 그 회원이 쓴 댓글, 그 회원 기록에 달린 댓글을 대상으로 한 신고를 지운다. 대상 행을 지우기 전에 호출한다. */
    @Modifying
    @Query("""
            delete from MobileContentReport report
            where (report.targetType = com.raota.mobile.ramenlog.domain.model.ReportTargetType.RAMEN_LOG
                    and report.targetId in (select log.id from MobileRamenLog log where log.userId = :userId))
                or (report.targetType = com.raota.mobile.ramenlog.domain.model.ReportTargetType.RAMEN_LOG_COMMENT
                    and report.targetId in (
                        select comment.id from MobileRamenLogComment comment
                        where comment.userId = :userId
                            or comment.ramenLogId in (select log.id from MobileRamenLog log where log.userId = :userId)))
            """)
    void deleteTargetingContentOf(Long userId);

}
