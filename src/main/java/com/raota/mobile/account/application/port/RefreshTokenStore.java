package com.raota.mobile.account.application.port;

import java.util.Optional;

/** 여러 기기에서 독립적으로 사용할 수 있는 리프레시 토큰을 관리한다. */
public interface RefreshTokenStore {

    String issue(Long userId);

    Optional<Long> consume(String refreshToken);

    void revoke(String refreshToken, Long userId);

    void revokeAll(Long userId);

}
