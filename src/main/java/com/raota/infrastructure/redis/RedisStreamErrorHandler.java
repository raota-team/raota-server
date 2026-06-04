package com.raota.infrastructure.redis;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ErrorHandler;

@Slf4j
@Component
public class RedisStreamErrorHandler implements ErrorHandler {

    @Override
    public void handleError(Throwable throwable) {
        Throwable rootCause = rootCauseOf(throwable);

        log.error(
                "Redis Stream listener error. exceptionType={}, rootCauseType={}, rootCauseMessage={}, retryHint={}",
                throwable.getClass().getName(),
                rootCause.getClass().getName(),
                Optional.ofNullable(rootCause.getMessage()).orElse("(no message)"),
                retryHint(rootCause),
                throwable
        );
    }

    private Throwable rootCauseOf(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private String retryHint(Throwable rootCause) {
        String message = Optional.ofNullable(rootCause.getMessage()).orElse("");

        if (message.contains("Command timed out")) {
            return "redis-read-timeout";
        }
        if (message.contains("Connection is already closed")) {
            return "connection-closed";
        }
        if (message.contains("LettuceConnectionFactory is STOPPING")) {
            return "application-shutdown";
        }

        return "check-listener-log-and-pending-state";
    }
}
