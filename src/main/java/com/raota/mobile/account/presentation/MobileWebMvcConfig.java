package com.raota.mobile.account.presentation;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 모바일 회원 ID를 받는 컨트롤러 매개변수를 해석한다. */
@Configuration
public class MobileWebMvcConfig implements WebMvcConfigurer {

    private final LoginUserArgumentResolver loginUserArgumentResolver;

    public MobileWebMvcConfig(LoginUserArgumentResolver loginUserArgumentResolver) {
        this.loginUserArgumentResolver = loginUserArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(loginUserArgumentResolver);
    }

}
