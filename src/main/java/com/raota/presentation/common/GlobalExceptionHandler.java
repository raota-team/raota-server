package com.raota.presentation.common;

import com.raota.account.infrastructure.auth.AuthenticationRequiredException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
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
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationRequired(
            AuthenticationRequiredException exception,
            HttpServletRequest request
    ) {
        log.warn("Authentication required. method={}, uri={}, query={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                exception.getMessage(),
                exception);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail(exception.getMessage()));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException exception,
            HttpServletRequest request
    ) {
        log.warn("Access denied. method={}, uri={}, message={}",
                request.getMethod(), request.getRequestURI(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.fail(exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        log.warn("Illegal argument. method={}, uri={}, query={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                exception.getMessage(),
                exception);
        return ResponseEntity.badRequest().body(ApiResponse.fail(exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(
            IllegalStateException exception,
            HttpServletRequest request
    ) {
        log.warn("Illegal state. method={}, uri={}, query={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                exception.getMessage(),
                exception);
        return ResponseEntity.badRequest().body(ApiResponse.fail(exception.getMessage()));
    }

    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEntityNotFound(
            jakarta.persistence.EntityNotFoundException exception,
            HttpServletRequest request
    ) {
        log.warn("Entity not found. method={}, uri={}, query={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                exception.getMessage(),
                exception);
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
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        log.warn("Method argument type mismatch. method={}, uri={}, query={}, name={}, value={}",
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                exception.getName(),
                exception.getValue(),
                exception);
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail("잘못된 요청 파라미터입니다: " + exception.getName()));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(
            BindException exception,
            HttpServletRequest request
    ) {
        String fieldName = exception.getFieldError() == null ? "unknown" : exception.getFieldError().getField();
        Object rejectedValue = exception.getFieldError() == null ? null : exception.getFieldError().getRejectedValue();
        log.warn("Bind exception. method={}, uri={}, query={}, field={}, rejectedValue={}",
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                fieldName,
                rejectedValue,
                exception);
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail("잘못된 요청 파라미터입니다: " + fieldName));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String fieldName = exception.getBindingResult().getFieldError() == null
                ? "unknown"
                : exception.getBindingResult().getFieldError().getField();
        String message = "잘못된 요청 파라미터입니다: " + fieldName;
        log.warn("Request body validation failed. method={}, uri={}, message={}",
                request.getMethod(), request.getRequestURI(), message);
        return ResponseEntity.badRequest().body(ApiResponse.fail(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(Exception exception) {
        log.error("Unhandled exception occurred: ", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("서버 내부 오류가 발생했습니다."));
    }
}
