package com.raota.web.ramenshop.presentation;

import com.raota.global.presentation.common.ApiResponse;
import com.raota.web.ramenshop.application.result.TodayPopularRamenShopResponse;
import com.raota.web.ramenshop.application.service.RamenShopViewRankingService;
import com.raota.web.ramenshop.presentation.contract.PopularRamenShopApi;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/discovery")
@RequiredArgsConstructor
public class PopularRamenShopController implements PopularRamenShopApi {

    private final RamenShopViewRankingService ramenShopViewRankingService;

    @Override
    @GetMapping("/popular-shops/today")
    public ResponseEntity<ApiResponse<List<TodayPopularRamenShopResponse>>> getTodayPopularShops(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(ApiResponse.success(ramenShopViewRankingService.getTodayPopularShops(limit)));
    }
}
