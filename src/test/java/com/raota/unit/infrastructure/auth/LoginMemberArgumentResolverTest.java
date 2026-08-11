package com.raota.unit.infrastructure.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.raota.account.domain.member.model.MemberRole;
import com.raota.account.infrastructure.auth.AuthenticatedMember;
import com.raota.account.infrastructure.auth.AuthenticationRequiredException;
import com.raota.account.infrastructure.auth.LoginMember;
import com.raota.account.infrastructure.auth.LoginMemberArgumentResolver;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

public class LoginMemberArgumentResolverTest {

    private LoginMemberArgumentResolver resolver;
    ModelAndViewContainer mavContainer;
    NativeWebRequest webRequest;
    WebDataBinderFactory binderFactory;

    @BeforeEach
    void setUp() {
        resolver = new LoginMemberArgumentResolver();
        mavContainer = mock(ModelAndViewContainer.class);
        webRequest = mock(NativeWebRequest.class);
        binderFactory = mock(WebDataBinderFactory.class);
    }
    @Test
    @DisplayName("@LoginMember 어노테이션과 Long 타입 파라미터가 있으면 지원한다")
    void supportsParameter_Success() throws NoSuchMethodException{
        MethodParameter methodParameter = getMethodParameter("supportedMethod",Long.class);

        boolean result = resolver.supportsParameter(methodParameter);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("@LoginMember 어노테이션이 없으면 지원하지 않는다.")
    void supportsParameter_fail() throws NoSuchMethodException {
        MethodParameter methodParameter = getMethodParameter("unsupportedMethodNoAnnotation",Long.class);

        boolean result = resolver.supportsParameter(methodParameter);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("타입이 Long이 아니면 지원하지 않는다.")
    void supportsParameter_is_not_long() throws NoSuchMethodException {
        MethodParameter methodParameter = getMethodParameter("unsupportedMethodWrongType",String.class);

        boolean result = resolver.supportsParameter(methodParameter);

        assertThat(result).isFalse();
    }

    @Test
    void 인증된_사용자_객체에서_유저_아이디를_반환한다() throws NoSuchMethodException {
        MethodParameter parameter = getMethodParameter("supportedMethod", Long.class);
        AuthenticatedMember member = AuthenticatedMember.of(1L, MemberRole.USER);
        Authentication authentication = new UsernamePasswordAuthenticationToken(member, null, member.authorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Object result = resolver.resolveArgument(parameter, mavContainer, webRequest, binderFactory);

        assertThat(result).isEqualTo(member.memberId());
    }

    @Test
    void 인증된_사용자가_아닐경우_에러발생() throws NoSuchMethodException {
        MethodParameter parameter = getMethodParameter("supportedMethod", Long.class);
        SecurityContextHolder.getContext().setAuthentication(new  UsernamePasswordAuthenticationToken("anonymous", null, null));

        assertThatThrownBy(() -> resolver.resolveArgument(parameter, mavContainer, webRequest, binderFactory))
                    .isInstanceOf(AuthenticationRequiredException.class);
    }


    //가짜 메소드 생성
    private void supportedMethod(@LoginMember Long memberId) {}
    private void unsupportedMethodNoAnnotation(Long memberId) {}
    private void unsupportedMethodWrongType(@LoginMember String memberId) {}

    private MethodParameter getMethodParameter(String methodName, Class<?> parameterType) throws NoSuchMethodException {
        Method method = this.getClass().getDeclaredMethod(methodName, parameterType);
        return new MethodParameter(method, 0); //첫 번째 파라미터를 가져옴
    }
}
