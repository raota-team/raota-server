package com.raota.global.presentation.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

    RequestIdFilter filter = new RequestIdFilter();
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void 요청_속성과_응답_헤더와_MDC에_같은_requestId를_넣는다() throws Exception {
        AtomicReference<String> mdcDuringChain = new AtomicReference<>();
        FilterChain chain = (req, res) -> mdcDuringChain.set(MDC.get(RequestIdFilter.MDC_KEY));

        filter.doFilter(request, response, chain);

        String requestId = response.getHeader(RequestIdFilter.HEADER);
        assertThat(requestId).isNotBlank();
        assertThat(RequestIdFilter.currentRequestId(request)).isEqualTo(requestId);
        assertThat(mdcDuringChain.get()).isEqualTo(requestId);
    }

    @Test
    void 체인이_끝나면_MDC를_지운다() throws Exception {
        filter.doFilter(request, response, (req, res) -> { });

        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void 체인이_예외를_던져도_MDC를_지운다() {
        FilterChain chain = (req, res) -> {
            throw new ServletException("boom");
        };

        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                .isInstanceOf(ServletException.class);
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void 이미_requestId가_있는_요청은_새로_만들지_않는다() throws Exception {
        request.setAttribute(RequestIdFilter.ATTRIBUTE, "existing-id");

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("existing-id");
    }

    @Test
    void 필터를_거치지_않은_요청의_requestId는_null이다() {
        assertThat(RequestIdFilter.currentRequestId(request)).isNull();
    }
}
