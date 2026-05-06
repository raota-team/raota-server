package com.raota.global.cache;

public record CacheInvalidationMessage(String cacheName,String key) {
}
