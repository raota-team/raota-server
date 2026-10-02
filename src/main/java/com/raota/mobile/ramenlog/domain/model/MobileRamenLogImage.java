package com.raota.mobile.ramenlog.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 기록의 사진을 앱이 정한 순서대로 보관한다. */
@Entity
@Table(name = "tb_v2_ramen_log_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MobileRamenLogImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ramen_log_id", nullable = false)
    private Long ramenLogId;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public static MobileRamenLogImage of(Long logId, String url, int order) {
        MobileRamenLogImage image = new MobileRamenLogImage();
        image.ramenLogId = logId;
        image.url = url;
        image.sortOrder = order;
        return image;
    }

}
