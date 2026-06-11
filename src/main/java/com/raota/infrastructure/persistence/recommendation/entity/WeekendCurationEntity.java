package com.raota.infrastructure.persistence.recommendation.entity;

import com.raota.domain.recommendation.model.WeekendCuration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

@Entity
@Table(name = "tb_weekend_curation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeekendCurationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer yearWeek;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ramen_type_id", nullable = false)
    private RamenTypeEntity ramenType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String customImageUrl;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public WeekendCurationEntity(Long id, Integer yearWeek, RamenTypeEntity ramenType, String reason, String customImageUrl, LocalDateTime createdAt) {
        this.id = id;
        this.yearWeek = yearWeek;
        this.ramenType = ramenType;
        this.reason = reason;
        this.customImageUrl = customImageUrl;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public static WeekendCurationEntity from(WeekendCuration domain) {
        return WeekendCurationEntity.builder()
                .id(domain.getId())
                .yearWeek(domain.getYearWeek())
                .ramenType(RamenTypeEntity.from(domain.getRamenType()))
                .reason(domain.getReason())
                .customImageUrl(domain.getCustomImageUrl())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    public WeekendCuration toDomain() {
        return WeekendCuration.builder()
                .id(this.id)
                .yearWeek(this.yearWeek)
                .ramenType(this.ramenType.toDomain())
                .reason(this.reason)
                .customImageUrl(this.customImageUrl)
                .createdAt(this.createdAt)
                .build();
    }
}
