package com.raota.domain.ramenShop.model;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_normal_menu")
@Builder
@AllArgsConstructor
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NormalMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ramen_shop_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private RamenShop ramenShop;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer price;

    @Builder.Default
    @Column(name = "is_signature", nullable = false)
    private Boolean isSignature = false;

    @Column(name = "image_url")
    private String imageUrl;

    public void setShop(RamenShop shop) {
        this.ramenShop = shop;
    }

    public Boolean getIsSignature() {
        return isSignature != null && isSignature;
    }
}
