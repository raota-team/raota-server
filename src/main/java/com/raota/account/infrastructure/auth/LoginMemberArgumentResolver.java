package com.raota.account.infrastructure.auth;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 컨트롤러 메소드 파라미터로 '@LoginMember Long memberId'를 사용할 수 있게 해주는 Resolver.
 * JwtAuthenticationFilter가 보안 컨텍스트에 저장해둔 인증 정보를 자동으로 꺼내서 주입한다.
 */
@Component
public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {

    /**
     * 특정 파라미터가 이 resolver에 의해 처리될 수 있는지 확인한다.
     * @LoginMember 어노테이션이 붙어있고, 타입이 Long인 경우에만 처리한다.
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        boolean hasLoginAnnotation = parameter.hasParameterAnnotation(LoginMember.class);
        boolean hasLongType = Long.class.isAssignableFrom(parameter.getParameterType());

        return hasLoginAnnotation && hasLongType;
    }

    /**
     * 실제 컨트롤러 메소드에 전달할 값을 결정한다.
     * SecurityContext에서 인증 객체를 꺼내고, 그 안에 담긴 AuthenticatedMember의 memberId를 반환한다.
     */
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        // 1. 보안 컨텍스트(SecurityContext)에서 현재의 인증 정보를 가져온다.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        LoginMember annotation = parameter.getParameterAnnotation(LoginMember.class);
        boolean required = (annotation != null) && annotation.required();
        
        // 2. 인증 정보가 없거나, 인증된 사용자 객체(AuthenticatedMember)가 아니면 예외를 발생시킨다.
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedMember member)) {
            if(required) {
                throw new AuthenticationRequiredException("로그인이 필요합니다.");
            }
            return null;
        }
        
        // 3. 인증 정보에서 실제 사용자 고유 ID(memberId)를 반환하여 컨트롤러 파라미터로 주입한다.
        return member.memberId();
    }
}
