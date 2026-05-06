package com.raota.global.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@RequiredArgsConstructor
@Component
public class CacheInvalidationListener implements MessageListener {

    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte @Nullable [] pattern) {
        try {
            CacheInvalidationMessage invalidationMessage = objectMapper.readValue(message.getBody(),
                    CacheInvalidationMessage.class);

            log.debug("Received cache invalidation: {}", invalidationMessage);

            Cache cache = cacheManager.getCache(invalidationMessage.cacheName());

            if (cache != null) {
                if ("ALL".equals(invalidationMessage.key())) {
                    cache.clear();
                    log.info("Cleared all entries in cache: {}", invalidationMessage.cacheName());
                } else {
                    cache.evict(invalidationMessage.key());
                    log.debug("Evicted key [{}] from cache: {}", invalidationMessage.key(), invalidationMessage.cacheName());
                }
            }
        } catch (JacksonException e) {
            log.error("Failed to parse cache invalidation message", e);
        }
    }
}
