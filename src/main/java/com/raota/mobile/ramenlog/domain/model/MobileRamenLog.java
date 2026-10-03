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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 회원이 방문한 매장에서 남긴 라멘 기록이다. */
@Entity
@Table(name = "tb_v2_ramen_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MobileRamenLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(name = "visited_at", nullable = false)
    private LocalDate visitedAt;

    @Column(name = "menu_name", nullable = false, length = 150)
    private String menuName;

    @Column(name = "ramen_type", nullable = false, length = 50)
    private String ramenType;

    @Column(name = "satisfaction_score")
    private Byte satisfactionScore;

    @Column(name = "broth_density_score")
    private Byte brothDensityScore;

    @Column(name = "noodle_firmness_score")
    private Byte noodleFirmnessScore;

    @Column(name = "topping_score")
    private Byte toppingScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "revisit_intention", length = 20)
    private RevisitIntention revisitIntention;

    @Column(nullable = false, length = 500)
    private String note;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "taste_note_codes", nullable = false, columnDefinition = "json")
    private List<String> tasteNoteCodes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LogVisibility visibility;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "comment_count", nullable = false)
    private int commentCount;

    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public static MobileRamenLog create(Long userId, Long shopId, LocalDate visitedAt, String menuName,
            String ramenType, MobileRamenLogScores scores, RevisitIntention revisitIntention, String note,
            List<String> tasteNoteCodes, LogVisibility visibility, String idempotencyKey, Instant now) {
        MobileRamenLog log = new MobileRamenLog();
        log.userId = userId;
        log.shopId = shopId;
        log.visitedAt = visitedAt;
        log.menuName = menuName.trim();
        log.ramenType = ramenType;
        log.applyScores(scores);
        log.revisitIntention = revisitIntention;
        log.note = note;
        log.tasteNoteCodes = List.copyOf(tasteNoteCodes);
        log.visibility = visibility;
        log.idempotencyKey = idempotencyKey;
        log.createdAt = now;
        log.updatedAt = now;
        return log;
    }

    public void edit(MobileRamenLogEdit edit, Instant now) {
        if (edit.visitedAt() != null) {
            this.visitedAt = edit.visitedAt();
        }
        if (edit.menuName() != null) {
            this.menuName = edit.menuName().trim();
        }
        if (edit.ramenType() != null) {
            this.ramenType = edit.ramenType();
        }
        if (edit.scores() != null) {
            applyScores(edit.scores());
        }
        if (edit.revisitIntention() != null) {
            this.revisitIntention = edit.revisitIntention();
        }
        if (edit.note() != null) {
            this.note = edit.note();
        }
        if (edit.tasteNoteCodes() != null) {
            this.tasteNoteCodes = List.copyOf(edit.tasteNoteCodes());
        }
        if (edit.visibility() != null) {
            this.visibility = edit.visibility();
        }
        this.updatedAt = now;
    }

    /** 매장 평균 만족도 집계에 쓰는 만족도다. 점수가 없는 기록은 null이다. */
    public Integer satisfaction() {
        return satisfactionScore == null ? null : satisfactionScore.intValue();
    }

    private void applyScores(MobileRamenLogScores scores) {
        this.satisfactionScore = (byte) scores.satisfaction();
        this.brothDensityScore = (byte) scores.brothDensity();
        this.noodleFirmnessScore = (byte) scores.noodleFirmness();
        this.toppingScore = (byte) scores.topping();
    }

    public void softDelete(Instant now) {
        this.deletedAt = now;
        this.updatedAt = now;
    }

}
