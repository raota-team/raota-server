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
 * 요청마다 requestId(UUID)를 만들어 request attribute, 응답 헤더 {@code X-Request-Id}, MDC에 같은 값을 넣는다.
 *
 * <p>Spring Security 필터 체인은 순서 -100으로 등록된다. 이 필터가 그보다 앞에 서야
 * 보안 체인이 직접 쓰는 401·403 응답에도 헤더가 붙으므로 최우선 순서로 등록한다.</p>
 *
 * <p>MDC는 스레드에 묶이고 톰캣은 스레드를 재사용하므로 요청이 끝나면 반드시 지운다.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String ATTRIBUTE = RequestIdFilter.class.getName() + ".requestId";
    public static final String MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        request.setAttribute(ATTRIBUTE, requestId);
        response.setHeader(HEADER, requestId);
        MDC.put(MDC_KEY, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    /**
     * 컨테이너가 /error로 재진입(ERROR 디스패치)할 때 응답이 초기화될 수 있으므로 그때도 다시 실행해 헤더를 되살린다.
     */
    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    /** 요청 처리 중 어디서든 현재 requestId를 읽는다. 필터를 거치지 않은 요청이면 null이다. */
    public static String currentRequestId(HttpServletRequest request) {
        return request.getAttribute(ATTRIBUTE) instanceof String requestId ? requestId : null;
    }

    // ERROR 디스패치로 같은 요청이 다시 들어오면 처음 만든 값을 그대로 쓴다.
    private static String resolveRequestId(HttpServletRequest request) {
        String existing = currentRequestId(request);
        return existing != null ? existing : UUID.randomUUID().toString();
    }
}
