package com.raota.agent.presentation.admin;

import com.raota.agent.application.evaluation.RagEvaluationCaseStatus;
import com.raota.agent.application.evaluation.RagEvaluationCaseType;
import com.raota.agent.application.evaluation.RagEvaluationRunService;
import com.raota.agent.application.evaluation.RagEvaluationRunService.CaseView;
import com.raota.agent.application.evaluation.RagEvaluationRunService.DatasetView;
import com.raota.agent.application.evaluation.RagEvaluationRunService.ReviewCommand;
import com.raota.agent.application.evaluation.RagEvaluationRunService.RunStart;
import com.raota.agent.application.evaluation.RagEvaluationRunService.RunView;
import com.raota.agent.application.evaluation.RagEvaluationSplit;
import com.raota.web.account.infrastructure.auth.LoginMember;
import com.raota.global.presentation.common.ApiResponse;
import com.raota.global.presentation.common.PageResponse;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/api/rag-evaluations")
@RequiredArgsConstructor
public class RagEvaluationAdminController {

    private final RagEvaluationRunService runService;

    private final tools.jackson.databind.ObjectMapper objectMapper;

    @GetMapping("/datasets")
    public ResponseEntity<ApiResponse<DatasetView>> datasets(@RequestParam(required = false) String version) {
        return ResponseEntity.ok(ApiResponse.success(runService.dataset(version)));
    }

    @PostMapping("/runs")
    public ResponseEntity<ApiResponse<RunStart>> start(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody(required = false) StartRequest request) {
        String datasetVersion = request == null ? null : request.datasetVersion();
        RagEvaluationSplit split = request == null || request.split() == null ? RagEvaluationSplit.DEV
                : request.split();
        RunStart result = runService.start(datasetVersion, split, idempotencyKey);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success("RAG 평가 실행을 시작했습니다.", result));
    }

    @GetMapping("/runs")
    public ResponseEntity<ApiResponse<?>> runs(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String cursor) {
        if (cursor != null && !cursor.isBlank()) {
            return ResponseEntity.ok(ApiResponse.success(runService.listCursor(cursor, size)));
        }
        return ResponseEntity.ok(ApiResponse.success(runService.list(page, size)));
    }

    @GetMapping("/runs/{runId}")
    public ResponseEntity<ApiResponse<RunView>> run(@PathVariable String runId) {
        return ResponseEntity.ok(ApiResponse.success(runService.get(runId)));
    }

    @GetMapping("/runs/{runId}/cases")
    public ResponseEntity<ApiResponse<PageResponse<CaseView>>> cases(@PathVariable String runId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) RagEvaluationCaseType type,
            @RequestParam(required = false) RagEvaluationCaseStatus status) {
        return ResponseEntity.ok(ApiResponse.success(runService.cases(runId, page, size, type, status)));
    }

    @PatchMapping("/runs/{runId}/cases/{caseId}/review")
    public ResponseEntity<ApiResponse<CaseView>> review(@PathVariable String runId, @PathVariable String caseId,
            @LoginMember Long reviewerMemberId, @Valid @RequestBody ReviewRequest request) {
        ReviewCommand command = new ReviewCommand(request.finalScore(), request.approved(), request.verdict(),
                request.opinion());
        return ResponseEntity
            .ok(ApiResponse.success("평가 사례 검수를 저장했습니다.", runService.review(runId, caseId, reviewerMemberId, command)));
    }

    @PostMapping("/runs/{runId}/finalize")
    public ResponseEntity<ApiResponse<RunView>> finalizeRun(@PathVariable String runId) {
        return ResponseEntity.ok(ApiResponse.success("RAG 평가 기준선을 확정했습니다.", runService.finalize(runId)));
    }

    @GetMapping("/runs/{runId}/export")
    public ResponseEntity<byte[]> export(@PathVariable String runId) {
        byte[] payload = objectMapper.writeValueAsString(runService.export(runId)).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=rag-evaluation-" + runId + ".json")
            .body(payload);
    }

    public record StartRequest(String datasetVersion, RagEvaluationSplit split) {
    }

    public record ReviewRequest(Integer finalScore, Boolean approved, String verdict, String opinion) {
    }

}
