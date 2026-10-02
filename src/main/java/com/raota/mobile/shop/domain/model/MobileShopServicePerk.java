package com.raota.mobile.shop.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 운영자가 확인한 매장 서비스 혜택이다. */
@Entity
@Table(name = "tb_v2_shop_service_perk")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class MobileShopServicePerk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Enumerated(EnumType.STRING)
    @Column(name = "perk_type", nullable = false, length = 30)
    private ServicePerkType perkType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ServicePerkStatus status;

    private Integer price;

    @Column(name = "condition_text", length = 1000)
    private String conditionText;

    @Column(name = "verified_at")
    private Instant verifiedAt;

}
