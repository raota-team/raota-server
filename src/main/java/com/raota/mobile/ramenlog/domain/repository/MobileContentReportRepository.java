package com.raota.mobile.ramenlog.domain.repository;

import com.raota.mobile.ramenlog.domain.model.MobileContentReport;
import com.raota.mobile.ramenlog.domain.model.ReportStatus;
import com.raota.mobile.ramenlog.domain.model.ReportTargetType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    Page<MobileContentReport> findByStatus(ReportStatus status, Pageable pageable);

    /** 대상별 열린 신고 수다. 각 행은 {@code [targetId, count]}다. */
    @Query("""
            select report.targetId, count(report) from MobileContentReport report
            where report.status = com.raota.mobile.ramenlog.domain.model.ReportStatus.OPEN
                and report.targetType = :targetType and report.targetId in :targetIds
            group by report.targetId
            """)
    List<Object[]> countOpenByTarget(ReportTargetType targetType, Collection<Long> targetIds);

    /** 대상의 열린 신고를 모두 닫고 닫은 수를 돌려준다. 이미 다른 관리자가 닫았으면 0이다. */
    @Modifying(flushAutomatically = true)
    @Query("""
            update MobileContentReport report set report.status = :status, report.resolvedAt = :resolvedAt
            where report.targetType = :targetType and report.targetId = :targetId
                and report.status = ReportStatus.OPEN
            """)
    int closeOpen(ReportTargetType targetType, Long targetId, ReportStatus status, Instant resolvedAt);

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
