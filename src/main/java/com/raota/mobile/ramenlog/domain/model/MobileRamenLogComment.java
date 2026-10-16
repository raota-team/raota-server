package com.raota.mobile.ramenlog.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 기록에 달린 한 단계 댓글이다. 삭제는 소프트 삭제다. */
@Entity
@Table(name = "tb_v2_ramen_log_comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MobileRamenLogComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ramen_log_id", nullable = false)
    private Long ramenLogId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public static MobileRamenLogComment create(Long ramenLogId, Long userId, String content, String idempotencyKey,
            Instant now) {
        MobileRamenLogComment comment = new MobileRamenLogComment();
        comment.ramenLogId = ramenLogId;
        comment.userId = userId;
        comment.content = content;
        comment.idempotencyKey = idempotencyKey;
        comment.createdAt = now;
        comment.updatedAt = now;
        return comment;
    }

    public void softDelete(Instant now) {
        this.deletedAt = now;
        this.updatedAt = now;
    }

}
