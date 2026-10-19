package com.raota.mobile.ramenlog.presentation.admin;

import com.raota.mobile.common.page.OffsetPage;
import com.raota.mobile.common.presentation.response.MobileApiResponse;
import com.raota.mobile.ramenlog.application.result.MobileAdminReport;
import com.raota.mobile.ramenlog.application.result.MobileAdminReportResolutionResult;
import com.raota.mobile.ramenlog.application.service.MobileContentReportAdminService;
import com.raota.mobile.ramenlog.domain.model.ReportStatus;
import com.raota.mobile.ramenlog.presentation.admin.request.MobileResolveReportRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 웹(raota-front)의 앱 라운지 신고 처리 API다. 웹 관리자 인증({@code /admin/**} ADMIN)을 쓰고 응답은 v2
 * 형식이다.
 */
@RestController
@RequestMapping("/admin/api/v2/reports")
@RequiredArgsConstructor
public class MobileContentReportAdminController {

    private final MobileContentReportAdminService reports;

    @GetMapping
    public MobileApiResponse<OffsetPage<MobileAdminReport>> list(
            @RequestParam(defaultValue = "OPEN") ReportStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(OffsetPage.MAX_SIZE) int size) {
        return MobileApiResponse.success(reports.list(status, page, size));
    }

    @PostMapping("/{reportId:[0-9]+}/resolve")
    public MobileApiResponse<MobileAdminReportResolutionResult> resolve(@PathVariable Long reportId,
            @RequestBody @Valid MobileResolveReportRequest request) {
        return MobileApiResponse.success(reports.resolve(reportId, request.action()));
    }

}
