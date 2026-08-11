package com.raota.account.infrastructure.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.redis.auth")
public record AuthRedisProperties(
        String refreshTokenKeyPrefix,
        String refreshMemberKeyPrefix
) {
}
