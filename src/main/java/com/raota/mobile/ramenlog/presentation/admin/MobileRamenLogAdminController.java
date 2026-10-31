package com.raota.mobile.ramenlog.presentation.admin;

import com.raota.mobile.common.page.OffsetPage;
import com.raota.mobile.common.presentation.response.MobileApiResponse;
import com.raota.mobile.ramenlog.application.result.MobileAdminRamenLogCountsResult;
import com.raota.mobile.ramenlog.application.result.MobileAdminRamenLogDeletionResult;
import com.raota.mobile.ramenlog.application.result.MobileAdminRamenLogDetailResult;
import com.raota.mobile.ramenlog.application.result.MobileAdminRamenLogResult;
import com.raota.mobile.ramenlog.application.service.MobileRamenLogAdminService;
import com.raota.mobile.ramenlog.domain.model.LogVisibility;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 웹(raota-front)의 앱 기록 관리 API다. 비공개 기록은 내용을 가리고 개수만 보여 주며, 공개 기록만 운영자 삭제한다. 웹 관리자 인증
 * ({@code /admin/**} ADMIN)을 쓰고 응답은 v2 형식이다.
 */
@RestController
@RequestMapping("/admin/api/v2/ramen-logs")
@RequiredArgsConstructor
public class MobileRamenLogAdminController {

    private final MobileRamenLogAdminService ramenLogAdminService;

    @GetMapping
    public MobileApiResponse<OffsetPage<MobileAdminRamenLogResult>> list(
            @RequestParam(required = false) LogVisibility visibility,
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long shopId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(OffsetPage.MAX_SIZE) int size) {
        return MobileApiResponse.success(ramenLogAdminService.list(visibility, keyword, userId, shopId, page, size));
    }

    @GetMapping("/counts")
    public MobileApiResponse<MobileAdminRamenLogCountsResult> counts(@RequestParam Long userId) {
        return MobileApiResponse.success(ramenLogAdminService.counts(userId));
    }

    @GetMapping("/{logId}")
    public MobileApiResponse<MobileAdminRamenLogDetailResult> detail(@PathVariable Long logId) {
        return MobileApiResponse.success(ramenLogAdminService.detail(logId));
    }

    @DeleteMapping("/{logId}")
    public MobileApiResponse<MobileAdminRamenLogDeletionResult> delete(@PathVariable Long logId) {
        return MobileApiResponse.success(ramenLogAdminService.delete(logId));
    }
}
