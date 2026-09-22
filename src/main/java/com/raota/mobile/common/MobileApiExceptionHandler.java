package com.raota.mobile.common;

import com.raota.global.presentation.common.RequestIdFilter;
import com.raota.global.presentation.v2.InvalidCursorException;
import com.raota.global.presentation.v2.V2ApiResponse;
import com.raota.global.presentation.v2.V2Error;
import com.raota.global.presentation.v2.V2ErrorCode;
import com.raota.global.presentation.v2.V2FieldError;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * `com.raota.mobile` 컨트롤러의 예외를 v2 응답 형식으로 바꾼다.
 *
 * <p>범위를 mobile 패키지로 제한해 v1 응답 형식을 건드리지 않는다. 우선순위를 명시하는 이유는
 * v1 전역 처리기가 {@code Exception} 전체를 잡기 때문이다. 순서를 지정하지 않으면 패키지 스캔
 * 순서에 따라 v1 처리기가 먼저 선택되어 v2 오류가 v1 형식으로 나간다.</p>
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.raota.mobile")
public class MobileApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<V2ApiResponse<Void>> handleInvalidBody(MethodArgumentNotValidException exception) {
        return respond(V2ErrorCode.VALIDATION_ERROR, "입력값을 확인해 주세요.", fieldErrors(exception.getBindingResult().getFieldErrors()));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<V2ApiResponse<Void>> handleBindFailure(BindException exception) {
        return respond(V2ErrorCode.VALIDATION_ERROR, "입력값을 확인해 주세요.", fieldErrors(exception.getFieldErrors()));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<V2ApiResponse<Void>> handleMalformedRequest(Exception exception) {
        log.debug("v2 요청 형식 오류", exception);
        return respond(V2ErrorCode.VALIDATION_ERROR, "요청 형식이 올바르지 않습니다.", List.of());
    }

    @ExceptionHandler(InvalidCursorException.class)
    public ResponseEntity<V2ApiResponse<Void>> handleInvalidCursor(InvalidCursorException exception) {
        return respond(V2ErrorCode.INVALID_CURSOR, exception.getMessage(), List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<V2ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException exception) {
        return respond(V2ErrorCode.VALIDATION_ERROR, exception.getMessage(), List.of());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<V2ApiResponse<Void>> handleNotFound(EntityNotFoundException exception) {
        return respond(V2ErrorCode.RESOURCE_NOT_FOUND, exception.getMessage(), List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<V2ApiResponse<Void>> handleUnexpected(Exception exception) {
        log.error("v2 처리되지 않은 예외", exception);
        return respond(V2ErrorCode.INTERNAL_ERROR, "요청을 처리하지 못했습니다.", List.of());
    }

    private ResponseEntity<V2ApiResponse<Void>> respond(V2ErrorCode code, String message, List<V2FieldError> fields) {
        V2Error error = V2Error.of(code, message == null || message.isBlank() ? code.name() : message, fields);
        return ResponseEntity.status(code.status())
                .body(V2ApiResponse.failure(error, RequestIdFilter.currentRequestId()));
    }

    private List<V2FieldError> fieldErrors(List<FieldError> fieldErrors) {
        return fieldErrors.stream()
                .map(fieldError -> new V2FieldError(
                        fieldError.getField(),
                        V2ErrorCode.VALIDATION_ERROR.name(),
                        fieldError.getDefaultMessage()
                ))
                .toList();
    }
}
