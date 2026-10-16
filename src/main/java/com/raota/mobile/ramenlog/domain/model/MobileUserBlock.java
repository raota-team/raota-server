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

/** 회원이 다른 회원의 글과 댓글을 보지 않기로 한 기록이다. */
@Entity
@Table(name = "tb_v2_user_block")
@IdClass(MobileUserBlock.Key.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MobileUserBlock {

    @Id
    @Column(name = "blocker_id")
    private Long blockerId;

    @Id
    @Column(name = "blocked_id")
    private Long blockedId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public record Key(Long blockerId, Long blockedId) implements Serializable {
    }

}
