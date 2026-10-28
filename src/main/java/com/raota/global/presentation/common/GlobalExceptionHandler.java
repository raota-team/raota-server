package com.raota.global.presentation.common;

import com.raota.global.file.UploadTicketLimitExceededException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 4xx는 클라이언트 오류라 INFO로 남긴다. 운영 logback은 WARN 이상을 Discord로 보내서, WARN이면 요청 정보가 그대로 전송된다.
    // IllegalStateException은 서버 상태 오류일 수 있어 WARN을 유지한다.

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException exception, HttpServletRequest request) {
        log.info(
                "Access denied. method={}, uri={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                exception.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.fail(exception.getMessage()));
    }

    @ExceptionHandler(UploadTicketLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleUploadTicketLimit(
            UploadTicketLimitExceededException exception, HttpServletRequest request) {
        log.info("Upload ticket limit exceeded. uri={}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(ApiResponse.fail(exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(
            IllegalArgumentException exception, HttpServletRequest request) {
        log.info(
                "Illegal argument. method={}, uri={}, query={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                exception.getMessage(),
                exception);
        return ResponseEntity.badRequest().body(ApiResponse.fail(exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(
            IllegalStateException exception, HttpServletRequest request) {
        log.warn(
                "Illegal state. method={}, uri={}, query={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                exception.getMessage(),
                exception);
        return ResponseEntity.badRequest().body(ApiResponse.fail(exception.getMessage()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEntityNotFound(
            EntityNotFoundException exception, HttpServletRequest request) {
        log.info(
                "Entity not found. method={}, uri={}, query={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                exception.getMessage(),
                exception);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(exception.getMessage()));
    }

    @ExceptionHandler(AiResponseFormatException.class)
    public ResponseEntity<ApiResponse<Void>> handleAiResponseFormat(
            AiResponseFormatException exception, HttpServletRequest request) {
        log.error(
                "AI response could not be converted. method={}, uri={}",
                request.getMethod(),
                request.getRequestURI(),
                exception);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.fail("AI 응답을 처리할 수 없습니다. 잠시 후 다시 시도해 주세요."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFound(NoResourceFoundException exception) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        log.info(
                "Method not supported. method={}, uri={}, remoteAddr={}, userAgent={}, supportedMethods={}",
                request.getMethod(),
                request.getRequestURI(),
                request.getRemoteAddr(),
                request.getHeader("User-Agent"),
                Arrays.toString(exception.getSupportedMethods()));
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ApiResponse.fail("지원하지 않는 HTTP 메서드입니다."));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        log.info(
                "Method argument type mismatch. method={}, uri={}, query={}, name={}, value={}",
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                exception.getName(),
                exception.getValue(),
                exception);
        return ResponseEntity.badRequest().body(ApiResponse.fail("잘못된 요청 파라미터입니다: " + exception.getName()));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException exception, HttpServletRequest request) {
        String fieldName = exception.getFieldError() == null
                ? "unknown"
                : exception.getFieldError().getField();
        Object rejectedValue = exception.getFieldError() == null
                ? null
                : exception.getFieldError().getRejectedValue();
        log.info(
                "Bind exception. method={}, uri={}, query={}, field={}, rejectedValue={}",
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                fieldName,
                rejectedValue,
                exception);
        return ResponseEntity.badRequest().body(ApiResponse.fail("잘못된 요청 파라미터입니다: " + fieldName));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        String fieldName = exception.getBindingResult().getFieldError() == null
                ? "unknown"
                : exception.getBindingResult().getFieldError().getField();
        String message = "잘못된 요청 파라미터입니다: " + fieldName;
        log.info(
                "Request body validation failed. method={}, uri={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                message);
        return ResponseEntity.badRequest().body(ApiResponse.fail(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(Exception exception) {
        log.error("Unhandled exception occurred: ", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("서버 내부 오류가 발생했습니다."));
    }
}
