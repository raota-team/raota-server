package com.raota.domain.auth.store;

import com.raota.domain.auth.model.RefreshToken;
import com.raota.domain.auth.repository.RefreshTokenRepository;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.auth.refresh-token",
        name = "store-type",
        havingValue = "jpa",
        matchIfMissing = true
)
public class JpaRefreshTokenStore implements RefreshTokenStore {

    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public Optional<StoredRefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token).map(this::toStoredToken);
    }

    @Override
    public Optional<StoredRefreshToken> findByMemberId(Long memberId) {
        return refreshTokenRepository.findByMemberId(memberId).map(this::toStoredToken);
    }

    @Override
    public void save(Long memberId, String token, Instant expiresAt) {
        refreshTokenRepository.findByMemberId(memberId)
                .ifPresentOrElse(
                        refreshToken -> refreshToken.rotate(token, expiresAt),
                        () -> refreshTokenRepository.save(RefreshToken.builder()
                                .memberId(memberId)
                                .token(token)
                                .expiryDate(expiresAt)
                                .build())
                );
    }

    @Override
    public void deleteByToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }

    private StoredRefreshToken toStoredToken(RefreshToken refreshToken) {
        return new StoredRefreshToken(
                refreshToken.getMemberId(),
                refreshToken.getToken(),
                refreshToken.getExpiresAt()
        );
    }
}
