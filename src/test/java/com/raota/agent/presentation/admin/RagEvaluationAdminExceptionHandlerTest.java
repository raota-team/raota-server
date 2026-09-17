package com.raota.agent.presentation.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.agent.application.evaluation.RagEvaluationAlreadyRunningException;
import com.raota.agent.application.evaluation.RagEvaluationRunService;
import com.raota.global.presentation.common.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

class RagEvaluationAdminExceptionHandlerTest {

    @Test
    @DisplayName("이미 실행 중인 평가가 있으면 409와 오류 코드를 반환한다")
    void alreadyRunningReturnsConflict() throws Exception {
        RagEvaluationRunService runService = mock(RagEvaluationRunService.class);
        given(runService.start(any(), any(), anyString())).willThrow(new RagEvaluationAlreadyRunningException());
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new RagEvaluationAdminController(runService, JsonMapper.builder().build()))
                .setControllerAdvice(new GlobalExceptionHandler(), new RagEvaluationAdminExceptionHandler())
                .build();

        mockMvc.perform(post("/admin/api/rag-evaluations/runs")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"split\":\"DEV\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("FAIL"))
                .andExpect(jsonPath("$.message").value(
                        "EVALUATION_ALREADY_RUNNING: 이미 실행 중인 RAG 평가가 있습니다."));
    }
}
