package com.raota.presentation.api.ramenShop;

import com.raota.application.ramenShop.RamenShopInfoService;
import com.raota.presentation.api.ramenShop.response.RecentVerifiedShopResponse;
import com.raota.presentation.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "홈 화면 - 라멘 가게", description = "홈 화면용 라멘 가게 API")
@RestController
@RequestMapping("/api/v1/shops")
@RequiredArgsConstructor
public class RamenShopV1ApiController {

    private final RamenShopInfoService ramenShopInfoService;

    @Operation(summary = "최근 사진 인증된 라멘집 조회", description = "최근에 사진 리뷰가 작성된 라멘집 목록을 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    @GetMapping("/recent-verified")
    public ResponseEntity<ApiResponse<List<RecentVerifiedShopResponse>>> getRecentVerifiedShops(
            @Parameter(description = "가져올 개수", example = "4")
            @RequestParam(defaultValue = "4") int limit) {
        return ResponseEntity.ok(ApiResponse.success(ramenShopInfoService.getRecentVerifiedShops(limit)));
    }
}
