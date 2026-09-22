package com.raota.global.presentation.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청마다 식별자를 만들어 로그(MDC)·응답 헤더·응답 본문 meta에 같은 값을 넣는다.
 *
 * <p>v1·v2 요청 모두에 적용한다. 로그와 사용자 문의를 연결하는 용도이며 재시도는 새 값을 받는다.
 * 비동기 실행으로 넘어가면 MDC는 전파되지 않으므로, 필요한 곳에서는 값을 명시적으로 넘긴다.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    /**
     * 현재 요청의 식별자를 반환한다. 필터 밖(스케줄러 등)에서는 빈 문자열이다.
     */
    public static String currentRequestId() {
        String requestId = MDC.get(MDC_KEY);
        return requestId == null ? "" : requestId;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER_NAME, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
