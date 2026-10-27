package com.raota.agent.presentation.admin;

import com.raota.agent.application.mobile.MobileRecommendationStatsService;
import com.raota.agent.application.mobile.result.MobileRecommendationStats;
import com.raota.global.presentation.common.ApiResponse;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 웹의 "AI 추천 성과"다. 웹 관리자 인증(`/admin/**` ADMIN)과 v1 응답 형식을 쓴다. */
@RestController
@RequestMapping("/admin/api/v2/recommendations")
@RequiredArgsConstructor
public class MobileRecommendationStatsAdminController {

    private final MobileRecommendationStatsService stats;

    /** {@code from}·{@code to}는 한국 날짜(양 끝 포함)다. 없으면 오늘까지 최근 30일이다. */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<MobileRecommendationStats>> stats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(stats.stats(from, to)));
    }
}
