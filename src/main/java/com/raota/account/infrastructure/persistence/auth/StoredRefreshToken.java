package com.raota.account.infrastructure.persistence.auth;

import java.time.Instant;

public record StoredRefreshToken(
        Long memberId,
        String token,
        Instant expiresAt
) {
    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now) || expiresAt.equals(now);
    }
}
