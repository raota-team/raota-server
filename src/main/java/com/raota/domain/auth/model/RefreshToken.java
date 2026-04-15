package com.raota.domain.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_refresh_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false, unique = true)
    private Long memberId;

    @Column(nullable = false, unique = true, length = 200)
    private String token;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    @Builder
    public RefreshToken(Long memberId, String token, Instant expiryDate) {
        this.memberId = memberId;
        this.token = token;
        this.expiryDate = expiryDate;
    }

    public void rotate(String token, Instant expiryDate) {
        this.token = token;
        this.expiryDate = expiryDate;
    }

    public boolean isExpired(Instant now) {
        return expiryDate.isBefore(now) || expiryDate.equals(now);
    }

    public Instant getExpiresAt() {
        return expiryDate;
    }
}
