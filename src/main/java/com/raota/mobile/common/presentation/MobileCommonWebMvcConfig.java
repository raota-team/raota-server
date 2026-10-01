package com.raota.mobile.common.presentation;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 모바일 API 컨트롤러의 필수·선택 회원 ID를 해석한다. */
@Configuration
public class MobileCommonWebMvcConfig implements WebMvcConfigurer {

    private final MobileLoginUserArgumentResolver loginUserArgumentResolver;

    public MobileCommonWebMvcConfig(MobileLoginUserArgumentResolver loginUserArgumentResolver) {
        this.loginUserArgumentResolver = loginUserArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(loginUserArgumentResolver);
    }

}
