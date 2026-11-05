package com.raota.mobile.ramenlog.presentation;

import com.raota.mobile.common.presentation.LoginUser;
import com.raota.mobile.common.presentation.response.MobileApiResponse;
import com.raota.mobile.ramenlog.application.result.MobileCurrentTasteReportResult;
import com.raota.mobile.ramenlog.application.result.MobileTasteReportSeenResult;
import com.raota.mobile.ramenlog.application.service.MobileTasteReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/members/me/taste-reports")
@RequiredArgsConstructor
public class MobileTasteReportController {

    private final MobileTasteReportService reports;

    @GetMapping("/current")
    public MobileApiResponse<MobileCurrentTasteReportResult> current(@LoginUser Long userId) {
        return MobileApiResponse.success(reports.current(userId));
    }

    @PostMapping("/{reportId}/seen")
    public MobileApiResponse<MobileTasteReportSeenResult> seen(@LoginUser Long userId, @PathVariable Long reportId) {
        return MobileApiResponse.success(reports.markSeen(userId, reportId));
    }
}
