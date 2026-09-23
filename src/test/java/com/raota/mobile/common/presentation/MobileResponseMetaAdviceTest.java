package com.raota.mobile.common.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.raota.global.presentation.common.RequestIdFilter;
import com.raota.mobile.common.presentation.response.MobileApiResponse;
import com.raota.mobile.common.presentation.response.MobileError;
import com.raota.mobile.common.error.MobileErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

class MobileResponseMetaAdviceTest {

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MobileResponseController())
            .setControllerAdvice(new MobileResponseMetaAdvice(), new MobileResponseExceptionHandler())
            .addFilters(new RequestIdFilter())
            .build();
    }

    @Test
    void 성공_응답의_meta_requestId를_응답_헤더와_같은_값으로_채운다() throws Exception {
        MvcResult result = mockMvc.perform(get("/mobile-response")).andExpect(status().isOk()).andReturn();

        assertThat(requestIdIn(result)).isEqualTo(headerOf(result));
    }

    @Test
    void ResponseEntity로_감싼_응답도_채운다() throws Exception {
        MvcResult result = mockMvc.perform(get("/mobile-response/entity")).andExpect(status().isCreated()).andReturn();

        assertThat(requestIdIn(result)).isEqualTo(headerOf(result));
    }

    @Test
    void 예외_처리기가_돌려준_실패_응답도_채운다() throws Exception {
        MvcResult result = mockMvc.perform(get("/mobile-response/fail")).andExpect(status().isNotFound()).andReturn();

        assertThat(requestIdIn(result)).isEqualTo(headerOf(result));
        assertThat(JsonPath.<Boolean>read(result.getResponse().getContentAsString(), "$.success")).isFalse();
    }

    @Test
    void 본문_없는_204는_헤더만_남기고_그대로_둔다() throws Exception {
        MvcResult result = mockMvc.perform(get("/mobile-response/none")).andExpect(status().isNoContent()).andReturn();

        assertThat(headerOf(result)).isNotBlank();
        assertThat(result.getResponse().getContentAsString()).isEmpty();
    }

    @Test
    void v2_응답_형식이_아닌_본문은_건드리지_않는다() throws Exception {
        MvcResult result = mockMvc.perform(get("/mobile-response/plain")).andExpect(status().isOk()).andReturn();

        assertThat(result.getResponse().getContentAsString()).isEqualTo("plain");
    }

    private static String headerOf(MvcResult result) {
        return result.getResponse().getHeader(RequestIdFilter.HEADER);
    }

    private static String requestIdIn(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.meta.requestId");
    }

    @RestController
    static class MobileResponseController {

        @GetMapping("/mobile-response")
        MobileApiResponse<String> success() {
            return MobileApiResponse.success("ok");
        }

        @GetMapping("/mobile-response/entity")
        ResponseEntity<MobileApiResponse<String>> entity() {
            return ResponseEntity.status(HttpStatus.CREATED).body(MobileApiResponse.success("ok"));
        }

        @GetMapping("/mobile-response/fail")
        MobileApiResponse<Void> fail() {
            throw new IllegalStateException("missing");
        }

        @GetMapping("/mobile-response/none")
        ResponseEntity<Void> none() {
            return ResponseEntity.noContent().build();
        }

        @GetMapping("/mobile-response/plain")
        String plain() {
            return "plain";
        }

    }

    @RestControllerAdvice
    static class MobileResponseExceptionHandler {

        @ExceptionHandler(IllegalStateException.class)
        ResponseEntity<MobileApiResponse<Void>> handle(IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(MobileApiResponse
                    .failure(MobileError.of(MobileErrorCode.RESOURCE_NOT_FOUND, exception.getMessage())));
        }

    }

}
