package com.raota.mobile.common.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.raota.global.presentation.common.GlobalExceptionHandler;
import com.raota.global.presentation.common.RequestIdFilter;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

class MobileExceptionAdviceTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MobileExceptionController())
            .setControllerAdvice(new GlobalExceptionHandler(), new MobileExceptionAdvice(),
                    new MobileResponseMetaAdvice())
            .addFilters(new RequestIdFilter())
            .build();
    }

    @Test
    void MobileException은_해당_오류_코드와_requestId로_응답한다() throws Exception {
        MvcResult result = expectFailure(
                get("/mobile-exception/conflict").header(RequestIdFilter.HEADER, "client-request-id"), 409, "CONFLICT");

        assertThat(JsonPath.<List<Map<String, Object>>>read(body(result), "$.error.fields")).isEmpty();
        assertThat(JsonPath.<String>read(body(result), "$.meta.requestId"))
            .isEqualTo(result.getResponse().getHeader(RequestIdFilter.HEADER));
    }

    @Test
    void 잘못된_JSON은_필드_오류_없이_400_VALIDATION_ERROR로_응답한다() throws Exception {
        MvcResult result = expectFailure(
                post("/mobile-exception/body").contentType(MediaType.APPLICATION_JSON).content("{"), 400,
                "VALIDATION_ERROR");

        assertThat(JsonPath.<List<Map<String, Object>>>read(body(result), "$.error.fields")).isEmpty();
    }

    @Test
    void 잘못된_열거형_값은_중첩_경로를_포함한_필드_오류로_응답한다() throws Exception {
        MvcResult result = expectFailure(post("/mobile-exception/enum").contentType(MediaType.APPLICATION_JSON)
            .content("{\"items\":[{\"type\":\"UNKNOWN\"}] }"), 400, "VALIDATION_ERROR");

        assertThat(JsonPath.<String>read(body(result), "$.error.fields[0].field")).isEqualTo("items[0].type");
        assertThat(JsonPath.<String>read(body(result), "$.error.fields[0].code")).isEqualTo("INVALID_FORMAT");
    }

    @Test
    void Valid_검증에_실패한_필드는_NotBlank_코드로_응답한다() throws Exception {
        MvcResult result = expectFailure(
                post("/mobile-exception/validated").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"),
                400, "VALIDATION_ERROR");

        assertThat(JsonPath.<List<Map<String, Object>>>read(body(result), "$.error.fields"))
            .anySatisfy(field -> assertThat(field).containsEntry("code", "NotBlank"));
    }

    @Test
    void 필수_요청_파라미터가_없으면_REQUIRED_필드_오류로_응답한다() throws Exception {
        MvcResult result = expectFailure(get("/mobile-exception/required"), 400, "VALIDATION_ERROR");

        assertThat(JsonPath.<String>read(body(result), "$.error.fields[0].field")).isEqualTo("name");
        assertThat(JsonPath.<String>read(body(result), "$.error.fields[0].code")).isEqualTo("REQUIRED");
    }

    @Test
    void 숫자_PathVariable_형식이_잘못되면_INVALID_FORMAT_필드_오류로_응답한다() throws Exception {
        MvcResult result = expectFailure(get("/mobile-exception/long/abc"), 400, "VALIDATION_ERROR");

        assertThat(JsonPath.<String>read(body(result), "$.error.fields[0].field")).isEqualTo("id");
        assertThat(JsonPath.<String>read(body(result), "$.error.fields[0].code")).isEqualTo("INVALID_FORMAT");
    }

    @Test
    void 최소값을_벗어난_요청_파라미터는_메서드_검증_오류로_응답한다() throws Exception {
        MvcResult result = expectFailure(get("/mobile-exception/min").param("count", "0"), 400, "VALIDATION_ERROR");

        assertThat(JsonPath.<String>read(body(result), "$.error.fields[0].field")).isEqualTo("count");
        assertThat(JsonPath.<String>read(body(result), "$.error.fields[0].code")).isEqualTo("Min");
    }

    @Test
    void 지원하지_않는_Content_Type은_400_VALIDATION_ERROR로_응답한다() throws Exception {
        expectFailure(post("/mobile-exception/body").contentType(MediaType.TEXT_PLAIN).content("plain"), 400,
                "VALIDATION_ERROR");
    }

    @Test
    void 접근_거부_예외는_403_FORBIDDEN으로_응답한다() throws Exception {
        expectFailure(get("/mobile-exception/forbidden"), 403, "FORBIDDEN");
    }

    @Test
    void 인증_예외는_401_UNAUTHORIZED로_응답한다() throws Exception {
        expectFailure(get("/mobile-exception/unauthorized"), 401, "UNAUTHORIZED");
    }

    @Test
    void 예기치_않은_예외는_고정된_500_INTERNAL_ERROR로_응답한다() throws Exception {
        MvcResult result = expectFailure(get("/mobile-exception/unexpected"), 500, "INTERNAL_ERROR");

        assertThat(body(result)).doesNotContain("secret-detail");
    }

    @Test
    void IllegalArgumentException은_v2에서_400으로_매핑하지_않고_500으로_응답한다() throws Exception {
        expectFailure(get("/mobile-exception/illegal-argument"), 500, "INTERNAL_ERROR");
    }

    private MvcResult expectFailure(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
            int expectedStatus, String expectedCode) throws Exception {
        MvcResult result = mockMvc.perform(request)
            .andExpect(status().is(expectedStatus))
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value(expectedCode))
            .andReturn();
        return result;
    }

    private static String body(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString();
    }

    @RestController
    static class MobileExceptionController {

        @GetMapping("/mobile-exception/conflict")
        void conflict() {
            throw new MobileException(MobileErrorCode.CONFLICT, "이미 존재합니다.");
        }

        @PostMapping("/mobile-exception/body")
        String body(@RequestBody BodyRequest request) {
            return "ok";
        }

        @PostMapping("/mobile-exception/enum")
        String enumValue(@RequestBody EnumRequest request) {
            return "ok";
        }

        @PostMapping("/mobile-exception/validated")
        String validated(@Valid @RequestBody ValidatedRequest request) {
            return "ok";
        }

        @GetMapping("/mobile-exception/required")
        String required(@RequestParam String name) {
            return name;
        }

        @GetMapping("/mobile-exception/long/{id}")
        String longPath(@PathVariable("id") Long id) {
            return id.toString();
        }

        @GetMapping("/mobile-exception/min")
        String min(@RequestParam @Min(1) int count) {
            return Integer.toString(count);
        }

        @GetMapping("/mobile-exception/forbidden")
        void forbidden() {
            throw new AccessDeniedException("private detail");
        }

        @GetMapping("/mobile-exception/unauthorized")
        void unauthorized() {
            throw new InsufficientAuthenticationException("private detail");
        }

        @GetMapping("/mobile-exception/unexpected")
        void unexpected() {
            throw new RuntimeException("secret-detail");
        }

        @GetMapping("/mobile-exception/illegal-argument")
        void illegalArgument() {
            throw new IllegalArgumentException("bad");
        }

    }

    record BodyRequest(String value) {
    }

    record EnumRequest(List<EnumItem> items) {
    }

    record EnumItem(ItemType type) {
    }

    enum ItemType {

        FIRST

    }

    record ValidatedRequest(@NotBlank(message = "이름을 입력해 주세요.") String name) {
    }

}
