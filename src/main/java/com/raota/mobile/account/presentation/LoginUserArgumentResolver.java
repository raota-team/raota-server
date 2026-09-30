package com.raota.mobile.account.presentation;

import com.raota.mobile.account.application.port.AuthenticatedMobileUser;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** 보안 컨텍스트의 모바일 회원 ID를 {@link LoginUser} 인자로 전달한다. */
@Component
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginUser.class) && parameter.getParameterType() == Long.class;
    }

    @Override
    public Long resolveArgument(MethodParameter parameter, ModelAndViewContainer modelAndViewContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedMobileUser principal)) {
            throw new MobileException(MobileErrorCode.UNAUTHORIZED, "인증이 필요합니다.");
        }
        return principal.userId();
    }

}
