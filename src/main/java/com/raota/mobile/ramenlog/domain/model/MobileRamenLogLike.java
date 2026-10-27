package com.raota.mobile.ramenlog.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 회원이 기록에 남긴 공감이다. 회원·기록 쌍마다 한 행만 있다. */
@Entity
@Table(name = "tb_v2_ramen_log_like")
@IdClass(MobileRamenLogLike.Key.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MobileRamenLogLike {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "ramen_log_id")
    private Long ramenLogId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public record Key(Long userId, Long ramenLogId) implements Serializable {}
}
