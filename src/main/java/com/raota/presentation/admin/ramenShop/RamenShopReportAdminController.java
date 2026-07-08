package com.raota.presentation.admin.ramenShop;

import com.raota.application.admin.ramenShop.RamenShopReportAdminService;
import com.raota.domain.ramenShop.model.RamenShopReportType;
import com.raota.presentation.admin.ramenShop.response.RamenShopReportAdminResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/ramen-shop-reports")
@RequiredArgsConstructor
public class RamenShopReportAdminController {

    private final RamenShopReportAdminService ramenShopReportAdminService;

    @GetMapping
    public String reports(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) RamenShopReportType reportType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            Model model
    ) {
        Page<RamenShopReportAdminResponse> reports = ramenShopReportAdminService.getReports(
                keyword,
                reportType,
                page,
                size
        );

        model.addAttribute("reports", reports);
        model.addAttribute("keyword", keyword);
        model.addAttribute("reportType", reportType);
        model.addAttribute("reportTypes", RamenShopReportType.values());
        return "admin/ramen-shop-reports";
    }
}
