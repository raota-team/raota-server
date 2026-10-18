package com.raota.agent.presentation.admin;

import com.raota.agent.application.mobile.MobileRecommendationLocationEvaluation;
import com.raota.agent.application.mobile.MobileRecommendationLocationEvaluation.Report;
import com.raota.global.presentation.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 일회용: v2 추천 위치 필터 전후 비교(#46). 결과를 이슈에 남긴 뒤 지운다. 웹 관리자 인증(`/admin/**` ADMIN)을 쓴다. */
@RestController
@RequestMapping("/admin/api/v2/recommendation-evaluations/location")
@RequiredArgsConstructor
public class MobileRecommendationEvaluationAdminController {

    private final MobileRecommendationLocationEvaluation evaluation;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> start() {
        evaluation.start();
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success("위치 필터 평가를 시작했습니다.", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Report>> latest() {
        return ResponseEntity.ok(ApiResponse.success(evaluation.latest().orElse(null)));
    }

}
