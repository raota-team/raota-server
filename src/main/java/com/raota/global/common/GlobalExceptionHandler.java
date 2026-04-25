package com.raota.global.common;

import com.raota.global.auth.AuthenticationRequiredException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationRequiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationRequired(AuthenticationRequiredException exception) {
        log.warn("Authentication required: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail(exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException exception) {
        log.warn("Illegal argument: {}", exception.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.fail(exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException exception) {
        log.warn("Illegal state: {}", exception.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.fail(exception.getMessage()));
    }

    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEntityNotFound(jakarta.persistence.EntityNotFoundException exception) {
        log.warn("Entity not found: {}", exception.getMessage());
        return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(ApiResponse.fail(exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(Exception exception) {
        log.error("Unhandled exception occurred: ", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("서버 내부 오류가 발생했습니다."));
    }
}
