package com.raota.infrastructure.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class CacheConfig {

    private final CacheProperties cacheProperties;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        cacheProperties.caches().forEach((cacheName, spec) ->
                cacheManager.registerCustomCache(
                        cacheName,
                        Caffeine.newBuilder()
                                .recordStats()
                                .expireAfterWrite(Duration.ofSeconds(spec.ttlSeconds()))
                                .maximumSize(spec.maximumSize())
                                .build()
                )
        );

        return cacheManager;
    }
}
