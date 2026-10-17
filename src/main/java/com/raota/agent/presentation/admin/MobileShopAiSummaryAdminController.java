package com.raota.agent.presentation.admin;

import com.raota.agent.application.mobile.MobileShopAiSummaryService;
import com.raota.agent.application.mobile.MobileShopAiSummaryService.BulkStart;
import com.raota.agent.application.mobile.MobileShopAiSummaryService.Result;
import com.raota.global.presentation.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 웹에서 v2 매장 AI 요약을 만든다. 웹 관리자 인증(`/admin/**` ADMIN)과 v1 응답 형식을 쓴다. */
@RestController
@RequestMapping("/admin/api/v2/shops")
@RequiredArgsConstructor
public class MobileShopAiSummaryAdminController {

    private final MobileShopAiSummaryService summaries;

    @PostMapping("/{shopId:[0-9]+}/ai-summary")
    public ResponseEntity<ApiResponse<Result>> summarize(@PathVariable Long shopId) {
        return ResponseEntity.ok(ApiResponse.success(summaries.summarize(shopId)));
    }

    @PostMapping("/ai-summary")
    public ResponseEntity<ApiResponse<BulkStart>> summarizeMissing() {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(ApiResponse.success("AI 요약이 없는 공개 매장의 요약 생성을 시작했습니다.", summaries.summarizeMissing()));
    }

}
