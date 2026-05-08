package com.raota.global.common;

import com.raota.global.auth.AuthenticationRequiredException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Arrays;

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

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFound(NoResourceFoundException exception) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Method not supported. method={}, uri={}, remoteAddr={}, userAgent={}, supportedMethods={}",
                request.getMethod(),
                request.getRequestURI(),
                request.getRemoteAddr(),
                request.getHeader("User-Agent"),
                Arrays.toString(exception.getSupportedMethods())
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.fail("지원하지 않는 HTTP 메서드입니다."));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException exception) {
        log.warn("Method argument type mismatch: name={}, value={}", exception.getName(), exception.getValue());
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail("잘못된 요청 파라미터입니다: " + exception.getName()));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException exception) {
        String fieldName = exception.getFieldError() == null ? "unknown" : exception.getFieldError().getField();
        Object rejectedValue = exception.getFieldError() == null ? null : exception.getFieldError().getRejectedValue();
        log.warn("Bind exception: field={}, rejectedValue={}", fieldName, rejectedValue);
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail("잘못된 요청 파라미터입니다: " + fieldName));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(Exception exception) {
        log.error("Unhandled exception occurred: ", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("서버 내부 오류가 발생했습니다."));
    }
}
