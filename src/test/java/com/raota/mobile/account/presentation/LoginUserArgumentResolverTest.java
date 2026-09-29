package com.raota.mobile.account.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

class LoginUserArgumentResolverTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 모바일_회원_인증_정보가_없으면_인증_오류를_반환한다() {
        SecurityContextHolder.clearContext();
        LoginUserArgumentResolver resolver = new LoginUserArgumentResolver();

        assertThatThrownBy(() -> resolver.resolveArgument(null, null, null, null)).isInstanceOfSatisfying(
                MobileException.class,
                exception -> assertThat(exception.code()).isEqualTo(MobileErrorCode.UNAUTHORIZED));
    }

}
