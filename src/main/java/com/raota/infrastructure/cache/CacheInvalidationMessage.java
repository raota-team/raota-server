package com.raota.infrastructure.cache;

public record CacheInvalidationMessage(String cacheName,String key) {
}
