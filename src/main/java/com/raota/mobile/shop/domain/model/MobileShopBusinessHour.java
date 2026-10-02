package com.raota.mobile.shop.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** ISO 요일 1(월)부터 7(일)까지 한 요일의 영업시간이다. */
@Entity
@Table(name = "tb_v2_shop_business_hour")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class MobileShopBusinessHour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(name = "day_of_week", nullable = false, columnDefinition = "tinyint")
    private byte dayOfWeek;

    @Column(name = "opens_at")
    private LocalTime opensAt;

    @Column(name = "closes_at")
    private LocalTime closesAt;

    @Column(name = "break_start")
    private LocalTime breakStart;

    @Column(name = "break_end")
    private LocalTime breakEnd;

    @Column(name = "last_order_at")
    private LocalTime lastOrderAt;

    @Column(name = "is_closed", nullable = false)
    private boolean closed;

}
