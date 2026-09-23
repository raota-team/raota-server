package com.raota.mobile.common.presentation;

import com.raota.global.presentation.common.RequestIdFilter;
import com.raota.mobile.common.presentation.response.MobileApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * v2 응답({@link MobileApiResponse})이 직렬화되기 직전에 {@code meta.requestId}를 채운다.
 *
 * <p>v2 controller와 예외 처리기에만 적용되도록 {@code com.raota.mobile} 패키지로 범위를 제한한다.</p>
 */
@RestControllerAdvice(basePackages = "com.raota.mobile")
public class MobileResponseMetaAdvice implements ResponseBodyAdvice<Object> {

    // 선언된 반환 타입은 ResponseEntity<...>일 수 있어 여기서는 거르지 않고 실제 본문으로 판단한다.
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        if (!(body instanceof MobileApiResponse<?> mobileResponse)) {
            return body;
        }
        HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
        return mobileResponse.withRequestId(RequestIdFilter.currentRequestId(servletRequest));
    }
}
