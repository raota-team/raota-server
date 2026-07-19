package com.raota.presentation.admin.ramenShop;

import com.raota.application.admin.ramenShop.RamenShopReportAdminService;
import com.raota.domain.ramenShop.model.RamenShopReportType;
import com.raota.presentation.admin.ramenShop.response.RamenShopReportAdminResponse;
import com.raota.presentation.common.ApiResponse;
import com.raota.presentation.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/api/ramen-shop-reports")
@RequiredArgsConstructor
public class RamenShopReportAdminController {

    private final RamenShopReportAdminService ramenShopReportAdminService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RamenShopReportAdminResponse>>> reports(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) RamenShopReportType reportType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        Page<RamenShopReportAdminResponse> reports = ramenShopReportAdminService.getReports(
                keyword,
                reportType,
                page,
                size
        );
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(reports)));
    }
}
