package com.raota.mobile.common.presentation;

import com.raota.global.presentation.common.RequestIdFilter;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.presentation.response.MobileApiResponse;
import com.raota.mobile.common.presentation.response.MobileError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.ObjectMapper;

/**
 * 컨트롤러가 결정되기 전에 발생한 v2 요청 오류를 처리한다.
 *
 * <p>advice는 선택한 예외를 조건부로 거절할 수 없어 전역 advice로 처리하면 v1 응답까지 변경된다.
 * 따라서 먼저 실행되고 거절할 수 있는 resolver를 사용한다. 컨트롤러 메서드가 결정된 경우에는
 * {@link MobileExceptionAdvice}가 처리하며, 그 밖의 요청 중 지정된 v2 오류만 이 resolver가 처리한다.</p>
 */
@Component
public class MobileFallbackExceptionResolver implements HandlerExceptionResolver, Ordered {

    private final ObjectMapper objectMapper;

    public MobileFallbackExceptionResolver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public ModelAndView resolveException(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!path.equals("/api/v2") && !path.startsWith("/api/v2/")) {
            return null;
        }
        if (handler instanceof HandlerMethod) {
            return null;
        }

        Failure failure = switch (ex) {
            case NoResourceFoundException _, NoHandlerFoundException _ ->
                    new Failure(MobileErrorCode.RESOURCE_NOT_FOUND, "요청한 API를 찾을 수 없습니다.");
            case HttpRequestMethodNotSupportedException methodException -> {
                Set<HttpMethod> supportedMethods = methodException.getSupportedHttpMethods();
                if (supportedMethods != null && !supportedMethods.isEmpty()) {
                    response.setHeader(HttpHeaders.ALLOW, supportedMethods.stream()
                            .map(HttpMethod::name)
                            .collect(Collectors.joining(", ")));
                }
                yield new Failure(MobileErrorCode.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다.");
            }
            case HttpMediaTypeNotSupportedException _ ->
                    new Failure(MobileErrorCode.VALIDATION_ERROR, "지원하지 않는 Content-Type입니다.");
            default -> null;
        };
        if (failure == null) {
            return null;
        }

        try {
            response.setStatus(failure.code().httpStatus().value());
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), MobileApiResponse
                    .failure(MobileError.of(failure.code(), failure.message()))
                    .withRequestId(RequestIdFilter.currentRequestId(request)));
            return new ModelAndView();
        } catch (IOException ignored) {
            return null;
        }
    }

    private record Failure(MobileErrorCode code, String message) {
    }
}
