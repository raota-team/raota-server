package com.raota.global.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheInvalidationPublisher {

    private final RedisTemplate<String, Object> cacheRedisTemplate;
    private final CacheProperties cacheProperties;

    public void publish(String cacheName, String key) {
        log.debug("Publishing cache invalidation - cache: {}, key: {}", cacheName, key);
        cacheRedisTemplate.convertAndSend(
                cacheProperties.invalidationTopic(),
                new CacheInvalidationMessage(cacheName, key)
        );
    }

    public void publishAll(String cacheName) {
        publish(cacheName, "ALL");
    }
}
