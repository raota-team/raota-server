package com.raota.global.presentation.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.raota.mobile.common.presentation.MobileExceptionAdvice;
import com.raota.mobile.common.presentation.MobileResponseMetaAdvice;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerV1ScopeTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new V1ExceptionController())
                .setControllerAdvice(
                        new GlobalExceptionHandler(),
                        new MobileExceptionAdvice(),
                        new MobileResponseMetaAdvice()
                )
                .build();
    }

    @Test
    void IllegalArgumentException은_v1_400_응답을_유지한다() throws Exception {
        assertV1Failure(get("/v1-exception/illegal-argument"), 400);
    }

    @Test
    void 일반_예외는_v1_500_응답을_유지한다() throws Exception {
        assertV1Failure(get("/v1-exception/unexpected"), 500);
    }

    private void assertV1Failure(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
            int expectedStatus
    ) throws Exception {
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().is(expectedStatus))
                .andReturn();
        String body = result.getResponse().getContentAsString();

        assertThat(JsonPath.<String>read(body, "$.status")).isEqualTo("FAIL");
        assertThat(JsonPath.<Boolean>read(body, "$.success")).isFalse();
        assertThat(JsonPath.<Map<String, Object>>read(body, "$")).doesNotContainKey("error");
    }

    @RestController
    static class V1ExceptionController {

        @GetMapping("/v1-exception/illegal-argument")
        void illegalArgument() {
            throw new IllegalArgumentException("bad");
        }

        @GetMapping("/v1-exception/unexpected")
        void unexpected() {
            throw new RuntimeException("bad");
        }
    }
}
