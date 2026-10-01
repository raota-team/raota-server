package com.raota.mobile.common.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.raota.mobile.common.MobilePrincipal;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

class MobileLoginUserArgumentResolverTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 모바일_회원_인증_정보가_없으면_필수_ID는_인증_오류다() throws Exception {
        MobileLoginUserArgumentResolver resolver = new MobileLoginUserArgumentResolver();
        MethodParameter required = parameter("required");

        assertThatThrownBy(() -> resolver.resolveArgument(required, null, null, null)).isInstanceOfSatisfying(
                MobileException.class,
                exception -> assertThat(exception.code()).isEqualTo(MobileErrorCode.UNAUTHORIZED));
    }

    @Test
    void 선택_ID는_익명일_때_null이고_인증되면_회원_ID다() throws Exception {
        MobileLoginUserArgumentResolver resolver = new MobileLoginUserArgumentResolver();
        MethodParameter optional = parameter("optional");
        assertThat(resolver.resolveArgument(optional, null, null, null)).isNull();

        MobilePrincipal principal = () -> 17L;
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        assertThat(resolver.resolveArgument(optional, null, null, null)).isEqualTo(17L);
    }

    private MethodParameter parameter(String name) throws Exception {
        Method method = getClass().getDeclaredMethod(name, Long.class);
        return new MethodParameter(method, 0);
    }

    private void required(@LoginUser Long userId) {
    }

    private void optional(@LoginUser(required = false) Long userId) {
    }

}
