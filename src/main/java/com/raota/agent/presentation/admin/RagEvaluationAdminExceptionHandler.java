package com.raota.agent.presentation.admin;

import com.raota.agent.application.evaluation.RagEvaluationAlreadyRunningException;
import com.raota.global.presentation.common.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = RagEvaluationAdminController.class)
public class RagEvaluationAdminExceptionHandler {

    @ExceptionHandler(RagEvaluationAlreadyRunningException.class)
    public ResponseEntity<ApiResponse<Void>> handleAlreadyRunning(RagEvaluationAlreadyRunningException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.fail(RagEvaluationAlreadyRunningException.CODE + ": " + exception.getMessage()));
    }
}
