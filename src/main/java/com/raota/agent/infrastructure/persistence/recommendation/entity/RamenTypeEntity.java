package com.raota.agent.infrastructure.persistence.recommendation.entity;

import com.raota.agent.domain.recommendation.model.RamenType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_ramen_type")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RamenTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String subTitle;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    @Builder
    public RamenTypeEntity(Long id, String name, String subTitle, String imageUrl) {
        this.id = id;
        this.name = name;
        this.subTitle = subTitle;
        this.imageUrl = imageUrl;
    }

    public static RamenTypeEntity from(RamenType domain) {
        return RamenTypeEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .subTitle(domain.getSubTitle())
                .imageUrl(domain.getImageUrl())
                .build();
    }

    public RamenType toDomain() {
        return RamenType.builder()
                .id(this.id)
                .name(this.name)
                .subTitle(this.subTitle)
                .imageUrl(this.imageUrl)
                .build();
    }
}
