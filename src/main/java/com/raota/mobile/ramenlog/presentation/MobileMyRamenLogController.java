package com.raota.mobile.ramenlog.presentation;

import com.raota.mobile.common.cursor.CursorPage;
import com.raota.mobile.common.presentation.LoginUser;
import com.raota.mobile.common.presentation.response.MobileApiResponse;
import com.raota.mobile.ramenlog.application.result.MobileRamenLogSummary;
import com.raota.mobile.ramenlog.application.result.MobileRamenLogSummaryItem;
import com.raota.mobile.ramenlog.application.service.MobileRamenLogQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/members/me/ramen-logs")
@RequiredArgsConstructor
public class MobileMyRamenLogController {

    private final MobileRamenLogQueryService logs;

    @GetMapping
    public MobileApiResponse<CursorPage<MobileRamenLogSummary>> list(@LoginUser Long userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(CursorPage.MAX_SIZE) int size) {
        return MobileApiResponse.success(logs.list(userId, cursor, size));
    }

    @GetMapping("/summary")
    public MobileApiResponse<List<MobileRamenLogSummaryItem>> summary(@LoginUser Long userId) {
        return MobileApiResponse.success(logs.summary(userId));
    }

}
