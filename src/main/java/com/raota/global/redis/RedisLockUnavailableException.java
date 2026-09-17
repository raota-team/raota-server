package com.raota.global.redis;

public class RedisLockUnavailableException extends RuntimeException {

    public RedisLockUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
