package com.raota.home.presentation;

import com.raota.global.presentation.common.ApiResponse;
import com.raota.home.application.HomeStatsService;
import com.raota.home.presentation.contract.HomeStatsApi;
import com.raota.home.presentation.response.HomeStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/discovery")
@RequiredArgsConstructor
public class HomeStatsController implements HomeStatsApi {

    private final HomeStatsService homeStatsService;

    @Override
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<HomeStatsResponse>> getHomeStats() {
        return ResponseEntity.ok(ApiResponse.success(homeStatsService.getStats()));
    }
}
