package com.raota.mobile.common.presentation;

import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import com.raota.mobile.common.presentation.response.MobileApiResponse;
import com.raota.mobile.common.presentation.response.MobileError;
import com.raota.mobile.common.presentation.response.MobileFieldError;
import jakarta.validation.Constraint;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.MismatchedInputException;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.raota.mobile")
public class MobileExceptionAdvice {

    private static final String VALIDATION_MESSAGE = "입력값을 확인해 주세요.";
    private static final String INVALID_FORMAT = "형식이 올바르지 않습니다.";

    @ExceptionHandler(MobileException.class)
    public ResponseEntity<MobileApiResponse<Void>> handleMobileException(MobileException exception) {
        if (exception.code().httpStatus().is4xxClientError()) {
            log.debug("Mobile request rejected: code={}", exception.code());
        } else {
            log.error("Mobile exception: code={}", exception.code(), exception);
        }
        return respond(exception.code(), exception.getMessage(), List.of());
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<MobileApiResponse<Void>> handleBindException(BindException exception) {
        log.debug("Mobile request validation failed");
        List<MobileFieldError> fields = exception.getFieldErrors().stream()
                .map(MobileExceptionAdvice::toFieldError)
                .toList();
        // 필드 형식이 없는 전역 ObjectError는 v2의 필드 오류 배열에서 제외한다.
        return respond(MobileErrorCode.VALIDATION_ERROR, VALIDATION_MESSAGE, fields);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<MobileApiResponse<Void>> handleMethodValidation(
            HandlerMethodValidationException exception
    ) {
        log.debug("Mobile method validation failed");
        List<MobileFieldError> fields = exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream().map(error ->
                        new MobileFieldError(
                                result.getMethodParameter().getParameterName(),
                                constraintCode(error, result.getMethodParameter()),
                                error.getDefaultMessage()
                        )))
                .toList();
        return respond(MobileErrorCode.VALIDATION_ERROR, VALIDATION_MESSAGE, fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<MobileApiResponse<Void>> handleUnreadableMessage(
            HttpMessageNotReadableException exception
    ) {
        log.debug("Mobile request body could not be read");
        List<MobileFieldError> fields = jacksonPath(exception).map(path -> List.of(
                new MobileFieldError(path, "INVALID_FORMAT", INVALID_FORMAT)
        )).orElseGet(List::of);
        return respond(MobileErrorCode.VALIDATION_ERROR, "요청 본문을 읽을 수 없습니다.", fields);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<MobileApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception
    ) {
        log.debug("Mobile request parameter has an invalid format: field={}", exception.getName());
        return respond(
                MobileErrorCode.VALIDATION_ERROR,
                VALIDATION_MESSAGE,
                List.of(new MobileFieldError(exception.getName(), "INVALID_FORMAT", INVALID_FORMAT))
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<MobileApiResponse<Void>> handleMissingParameter(
            MissingServletRequestParameterException exception
    ) {
        log.debug("Mobile request is missing a parameter: field={}", exception.getParameterName());
        return respond(
                MobileErrorCode.VALIDATION_ERROR,
                VALIDATION_MESSAGE,
                List.of(new MobileFieldError(exception.getParameterName(), "REQUIRED", "필수 값입니다."))
        );
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<MobileApiResponse<Void>> handleMissingHeader(MissingRequestHeaderException exception) {
        log.debug("Mobile request is missing a header: field={}", exception.getHeaderName());
        return respond(
                MobileErrorCode.VALIDATION_ERROR,
                VALIDATION_MESSAGE,
                List.of(new MobileFieldError(exception.getHeaderName(), "REQUIRED", "필수 값입니다."))
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<MobileApiResponse<Void>> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception
    ) {
        log.debug("Mobile request uses an unsupported Content-Type");
        return respond(MobileErrorCode.VALIDATION_ERROR, "지원하지 않는 Content-Type입니다.", List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Void> handleNotAcceptable(HttpMediaTypeNotAcceptableException exception) {
        log.debug("Mobile response cannot satisfy the requested Accept header");
        // 클라이언트가 JSON을 거부했으므로 JSON 실패 본문을 쓰지 않고 상태만 반환한다.
        return ResponseEntity.status(406).build();
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<MobileApiResponse<Void>> handleAccessDenied(AccessDeniedException exception) {
        log.debug("Mobile access denied");
        return respond(MobileErrorCode.FORBIDDEN, "접근 권한이 없습니다.", List.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<MobileApiResponse<Void>> handleAuthentication(AuthenticationException exception) {
        log.debug("Mobile authentication is required");
        return respond(MobileErrorCode.UNAUTHORIZED, "인증이 필요합니다.", List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MobileApiResponse<Void>> handleUnexpected(Exception exception) {
        log.error("Unhandled mobile exception", exception);
        return respond(MobileErrorCode.INTERNAL_ERROR, "서버 내부 오류가 발생했습니다.", List.of());
    }

    private ResponseEntity<MobileApiResponse<Void>> respond(
            MobileErrorCode code,
            String message,
            List<MobileFieldError> fields
    ) {
        return ResponseEntity.status(code.httpStatus())
                .body(MobileApiResponse.failure(MobileError.of(code, message, fields)));
    }

    private static MobileFieldError toFieldError(FieldError error) {
        return new MobileFieldError(error.getField(), error.getCode(), error.getDefaultMessage());
    }

    private static String constraintCode(MessageSourceResolvable error, MethodParameter parameter) {
        String[] codes = error.getCodes();
        if (codes != null && codes.length > 0) {
            return codes[codes.length - 1];
        }
        for (Annotation annotation : parameter.getParameterAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(Constraint.class)) {
                return annotation.annotationType().getSimpleName();
            }
        }
        return "VALIDATION_ERROR";
    }

    private static Optional<String> jacksonPath(HttpMessageNotReadableException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof MismatchedInputException mismatched && !mismatched.getPath().isEmpty()) {
                String path = formatPath(mismatched.getPath());
                if (!path.isEmpty()) {
                    return Optional.of(path);
                }
            }
        }
        return Optional.empty();
    }

    private static String formatPath(List<JacksonException.Reference> references) {
        StringBuilder path = new StringBuilder();
        for (JacksonException.Reference reference : references) {
            String propertyName = reference.getPropertyName();
            if (propertyName != null && !propertyName.isEmpty()) {
                if (!path.isEmpty()) {
                    path.append('.');
                }
                path.append(propertyName);
            }
            if (reference.getIndex() >= 0) {
                path.append('[').append(reference.getIndex()).append(']');
            }
        }
        return path.toString();
    }
}
