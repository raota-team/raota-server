package com.raota.web.account.presentation.auth;

import com.raota.web.account.infrastructure.auth.AuthenticationRequiredException;
import com.raota.global.presentation.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 인증 모듈 예외를 처리한다.
 *
 * <p>
 * {@code GlobalExceptionHandler}가 {@code Exception} 전체를 잡으므로 이 처리기가 먼저 적용되도록 순서를 지정한다.
 * 순서가 없으면 패키지 스캔 순서에 따라 전역 처리기가 먼저 선택되어 401 대신 500이 나간다.
 * </p>
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class AccountExceptionHandler {

    @ExceptionHandler(AuthenticationRequiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationRequired(AuthenticationRequiredException exception,
            HttpServletRequest request) {
        log.warn("Authentication required. method={}, uri={}, query={}, message={}", request.getMethod(),
                request.getRequestURI(), request.getQueryString(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail(exception.getMessage()));
    }

}
