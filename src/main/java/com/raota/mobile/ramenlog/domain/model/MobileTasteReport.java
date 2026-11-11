package com.raota.mobile.ramenlog.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 집계 숫자는 뽑을 때 굳히고 바꾸지 않는다. 취향 이름과 문장은 앱이 이 숫자로 만든다. */
@Entity
@Table(name = "tb_v2_taste_report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MobileTasteReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "cutoff_date", nullable = false)
    private LocalDate cutoffDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TasteReportKind kind;

    @Column(name = "bowl_count", nullable = false)
    private int bowlCount;

    @Column(name = "shop_count", nullable = false)
    private int shopCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "type_counts", nullable = false, columnDefinition = "json")
    private Map<String, Integer> typeCounts;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "axis_averages", nullable = false, columnDefinition = "json")
    private TasteAxisAverages axisAverages;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "top_shops", nullable = false, columnDefinition = "json")
    private List<TasteTopShop> topShops;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recent_log_ids", nullable = false, columnDefinition = "json")
    private List<Long> recentLogIds;

    @Column(name = "seen_at")
    private Instant seenAt;

    /** 매달 리포트 알림을 보내기 직전에 찍는다. 값이 있으면 다시 보내지 않는다. */
    @Column(name = "notified_at")
    private Instant notifiedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static MobileTasteReport draw(
            Long userId, TasteReportKind kind, LocalDate cutoffDate, MobileTasteReportFigures figures, Instant now) {
        MobileTasteReport report = new MobileTasteReport();
        report.userId = userId;
        report.kind = kind;
        report.cutoffDate = cutoffDate;
        report.bowlCount = figures.bowlCount();
        report.shopCount = figures.shopCount();
        report.typeCounts = figures.typeCounts();
        report.axisAverages = figures.axisAverages();
        report.topShops = figures.topShops();
        report.recentLogIds = figures.recentLogIds();
        report.createdAt = now;
        return report;
    }
}
