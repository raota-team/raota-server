package com.raota.mobile.ramenlog.domain.repository;

import com.raota.mobile.ramenlog.domain.model.MobileTasteReport;
import com.raota.mobile.ramenlog.domain.model.TasteReportKind;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MobileTasteReportRepository extends JpaRepository<MobileTasteReport, Long> {

    boolean existsByUserId(Long userId);

    Optional<MobileTasteReport> findFirstByUserIdOrderByCutoffDateDesc(Long userId);

    Optional<MobileTasteReport> findByIdAndUserId(Long id, Long userId);

    /** 최근 기준일 다음 날부터 {@code monthEnd}까지 방문한 기록이 {@code target}개 이상인 회원이다. */
    @Query(value = """
            SELECT latest.user_id
            FROM (SELECT user_id, MAX(cutoff_date) AS cutoff_date FROM tb_v2_taste_report GROUP BY user_id) latest
            WHERE latest.cutoff_date < :monthEnd
              AND (SELECT COUNT(*) FROM tb_v2_ramen_log log
                   WHERE log.user_id = latest.user_id AND log.deleted_at IS NULL
                     AND log.visited_at > latest.cutoff_date AND log.visited_at <= :monthEnd) >= :target
            ORDER BY latest.user_id
            """, nativeQuery = true)
    List<Long> findMonthlyDrawCandidates(LocalDate monthEnd, int target);

    List<MobileTasteReport> findByKindAndCutoffDateAndNotifiedAtIsNullAndSeenAtIsNull(
            TasteReportKind kind, LocalDate cutoffDate);

    /** 이미 본 리포트면 바꾸지 않는다. 동시에 와도 처음 시각 하나만 남는다. */
    @Modifying(clearAutomatically = true)
    @Query("""
            update MobileTasteReport report set report.seenAt = :now
            where report.id = :id and report.userId = :userId and report.seenAt is null
            """)
    int markSeen(Long id, Long userId, Instant now);

    @Modifying
    @Query("update MobileTasteReport report set report.notifiedAt = :now where report.id in :ids")
    void markNotified(List<Long> ids, Instant now);

    @Modifying
    @Query("delete from MobileTasteReport report where report.userId = :userId")
    void deleteForUser(Long userId);
}
