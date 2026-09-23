package com.raota.mobile.common.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.raota.global.presentation.common.RequestIdFilter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class MobileFallbackExceptionResolverTest {

    private final JsonMapper objectMapper = JsonMapper.builder().build();

    private final MobileFallbackExceptionResolver resolver = new MobileFallbackExceptionResolver(objectMapper);

    @Test
    void 컨트롤러가_없는_v2_경로는_requestId를_포함한_404_실패_응답을_쓴다() throws Exception {
        MockHttpServletRequest request = request("/api/v2/missing");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(resolver.resolveException(request, response, null, noResource("/api/v2/missing"))).isNotNull();

        JsonNode body = body(response);
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(MediaType.parseMediaType(response.getContentType()).isCompatibleWith(MediaType.APPLICATION_JSON))
            .isTrue();
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("error").get("code").asString()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(body.get("error").get("message").asString()).isEqualTo("요청한 API를 찾을 수 없습니다.");
        assertThat(body.get("data").isNull()).isTrue();
        assertThat(body.get("error").get("fields").isEmpty()).isTrue();
        assertThat(body.get("meta").get("requestId").asString())
            .isEqualTo(request.getAttribute(RequestIdFilter.ATTRIBUTE));
    }

    @Test
    void 컨트롤러가_없는_v2_경로의_지원하지_않는_메서드는_405와_Allow를_반환한다() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(resolver.resolveException(request("/api/v2/missing"), response, null,
                new HttpRequestMethodNotSupportedException("POST", List.of("GET"))))
            .isNotNull();

        assertThat(response.getStatus()).isEqualTo(405);
        assertThat(body(response).get("error").get("code").asString()).isEqualTo("METHOD_NOT_ALLOWED");
        assertThat(body(response).get("error").get("message").asString()).isEqualTo("지원하지 않는 HTTP 메서드입니다.");
        assertThat(response.getHeader(HttpHeaders.ALLOW)).contains("GET");
    }

    @Test
    void 컨트롤러가_없는_v2_경로의_지원하지_않는_Content_Type은_400_응답을_반환한다() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(resolver.resolveException(request("/api/v2/missing"), response, null,
                new HttpMediaTypeNotSupportedException(MediaType.TEXT_PLAIN, List.of(MediaType.APPLICATION_JSON))))
            .isNotNull();

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(body(response).get("error").get("code").asString()).isEqualTo("VALIDATION_ERROR");
        assertThat(body(response).get("error").get("message").asString()).isEqualTo("지원하지 않는 Content-Type입니다.");
    }

    @Test
    void 정확히_api_v2인_경로도_v2로_처리한다() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(resolver.resolveException(request("/api/v2"), response, null, noResource("/api/v2"))).isNotNull();
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(body(response).get("error").get("code").asString()).isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void 컨텍스트_경로를_제외한_경로로_v2_요청을_식별한다() throws Exception {
        MockHttpServletRequest request = request("/app/api/v2/missing");
        request.setContextPath("/app");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(resolver.resolveException(request, response, null, noResource("/app/api/v2/missing"))).isNotNull();
        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Test
    void 핸들러가_없는_v2_경로도_404로_처리한다() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(resolver.resolveException(request("/api/v2/missing"), response, null,
                new NoHandlerFoundException("GET", "/api/v2/missing", new HttpHeaders())))
            .isNotNull();
        assertThat(body(response).get("error").get("code").asString()).isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void v1_경로는_처리를_거절하고_응답을_수정하지_않는다() throws Exception {
        assertDeclined(request("/no-such"), null, noResource("/no-such"));
    }

    @Test
    void api_v2로_시작하지만_경로_구분자가_없는_경로는_거절한다() throws Exception {
        assertDeclined(request("/api/v2x/foo"), null, noResource("/api/v2x/foo"));
    }

    @Test
    void v2_컨트롤러_메서드가_결정된_요청은_advice에_맡긴다() throws Exception {
        HandlerMethod handler = new HandlerMethod(new ExampleHandler(),
                ExampleHandler.class.getDeclaredMethod("handle"));
        assertDeclined(request("/api/v2/missing"), handler, noResource("/api/v2/missing"));
    }

    @Test
    void 분류되지_않은_v2_예외는_기존_처리기에_맡긴다() throws Exception {
        assertDeclined(request("/api/v2/missing"), null, new RuntimeException("unexpected"));
    }

    private void assertDeclined(MockHttpServletRequest request, Object handler, Exception exception) {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(resolver.resolveException(request, response, handler, exception)).isNull();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsByteArray()).isEmpty();
        assertThat(response.getContentType()).isNull();
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setAttribute(RequestIdFilter.ATTRIBUTE, "resolver-unit-request-id");
        return request;
    }

    private NoResourceFoundException noResource(String path) {
        return new NoResourceFoundException(HttpMethod.GET, path, path);
    }

    private JsonNode body(MockHttpServletResponse response) throws Exception {
        return objectMapper.readTree(response.getContentAsByteArray());
    }

    private static class ExampleHandler {

        public void handle() {
        }

    }

}
