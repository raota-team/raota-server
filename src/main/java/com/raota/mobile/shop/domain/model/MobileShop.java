package com.raota.mobile.shop.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 앱이 직접 관리하는 매장 원본이다. v1 매장과 ID만 공유한다. */
@Entity
@Table(name = "tb_v2_shop")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class MobileShop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String name;

    @Column(name = "branch_name", length = 255)
    private String branchName;

    @Column(nullable = false, columnDefinition = "text")
    private String address;

    @Column(length = 1000)
    private String region;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(length = 100)
    private String phone;

    @Column(columnDefinition = "text")
    private String tagline;

    @Column(columnDefinition = "text")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ramen_types", nullable = false, columnDefinition = "json")
    private List<String> ramenTypes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "json")
    private List<String> tags;

    @Column(name = "instagram_url", length = 1000)
    private String instagramUrl;

    @Column(name = "reservation_url", length = 1000)
    private String reservationUrl;

    @Column(name = "website_url", length = 1000)
    private String websiteUrl;

    @Column(name = "naver_place_id", length = 100)
    private String naverPlaceId;

    @Column(name = "kakao_place_id", length = 100)
    private String kakaoPlaceId;

    @Column(name = "price_min")
    private Integer priceMin;

    @Column(name = "price_max")
    private Integer priceMax;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_status", nullable = false, length = 30)
    private MobileShopBusinessStatus businessStatus;

    @Column(name = "closed_days_text", length = 50)
    private String closedDaysText;

    @Column(name = "hours_verified_at")
    private Instant hoursVerifiedAt;

    @Column(name = "ai_review_summary", columnDefinition = "text")
    private String aiReviewSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_summary_keywords", nullable = false, columnDefinition = "json")
    private List<String> aiSummaryKeywords;

    @Column(name = "ai_summary_generated_at")
    private Instant aiSummaryGeneratedAt;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Column(name = "log_count", nullable = false)
    private int logCount;

    @Column(name = "satisfaction_score_sum", nullable = false)
    private int satisfactionScoreSum;

    @Column(name = "scored_log_count", nullable = false)
    private int scoredLogCount;

    @Column(name = "bookmark_count", nullable = false)
    private int bookmarkCount;

    @Column(name = "is_published", nullable = false)
    private boolean published;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

}
