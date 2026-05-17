package com.raota.infrastructure.cache;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.redis.cache")
public record CacheProperties(
        String invalidationTopic,
        Map<String, CacheSpec> caches
) {

    public record CacheSpec(
            long ttlSeconds,
            long maximumSize
    ) {
    }
}
