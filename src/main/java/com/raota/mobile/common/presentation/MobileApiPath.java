package com.raota.mobile.common.presentation;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * 요청이 모바일 v2 API({@code /api/v2}와 그 하위 경로)인지 판단한다.
 *
 * <p>
 * Spring MVC와 접근 규칙처럼 디코딩한 경로 조각으로 비교하므로 {@code /%61pi/v2}도 v2 요청으로 본다. 인증기, 보안 오류 응답기,
 * 예외 resolver가 같은 기준으로 v2 요청을 고르도록 이 판단을 함께 쓴다.
 * </p>
 */
public final class MobileApiPath {

    private static final RequestMatcher MATCHER = pathPattern("/api/v2/**");

    private MobileApiPath() {
    }

    public static boolean matches(HttpServletRequest request) {
        return MATCHER.matches(request);
    }

}
