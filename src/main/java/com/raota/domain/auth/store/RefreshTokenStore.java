package com.raota.domain.auth.store;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenStore {

    Optional<StoredRefreshToken> findByToken(String token);

    Optional<StoredRefreshToken> findByMemberId(Long memberId);

    void save(Long memberId, String token, Instant expiresAt);

    void deleteByToken(String token);
}
