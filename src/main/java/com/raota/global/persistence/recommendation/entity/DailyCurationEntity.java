package com.raota.global.persistence.recommendation.entity;

import com.raota.domain.recommendation.model.DailyCuration;
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
@Table(name = "tb_daily_curation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyCurationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_key", nullable = false, unique = true)
    private Integer dateKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ramen_type_id", nullable = false)
    private RamenTypeEntity ramenType;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String customImageUrl;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public DailyCurationEntity(Long id, Integer dateKey, RamenTypeEntity ramenType, String title, String reason, String customImageUrl, LocalDateTime createdAt) {
        this.id = id;
        this.dateKey = dateKey;
        this.ramenType = ramenType;
        this.title = title;
        this.reason = reason;
        this.customImageUrl = customImageUrl;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public static DailyCurationEntity from(DailyCuration domain) {
        return DailyCurationEntity.builder()
                .id(domain.getId())
                .dateKey(domain.getDateKey())
                .ramenType(RamenTypeEntity.from(domain.getRamenType()))
                .title(domain.getTitle())
                .reason(domain.getReason())
                .customImageUrl(domain.getCustomImageUrl())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    public DailyCuration toDomain() {
        return DailyCuration.builder()
                .id(this.id)
                .dateKey(this.dateKey)
                .ramenType(this.ramenType.toDomain())
                .title(this.title)
                .reason(this.reason)
                .customImageUrl(this.customImageUrl)
                .createdAt(this.createdAt)
                .build();
    }
}
