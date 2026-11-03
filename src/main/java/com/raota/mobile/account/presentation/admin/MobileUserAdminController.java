package com.raota.mobile.account.presentation.admin;

import com.raota.mobile.account.application.result.MobileAdminUserDetailResult;
import com.raota.mobile.account.application.result.MobileAdminUserResult;
import com.raota.mobile.account.application.service.MobileUserAdminService;
import com.raota.mobile.account.domain.model.MobileUserStatus;
import com.raota.mobile.common.page.OffsetPage;
import com.raota.mobile.common.presentation.response.MobileApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 웹(raota-front)의 앱 회원 조회 API다. 웹 회원({@code /admin/api/users})과 저장소가 달라 따로 둔다. 웹 관리자 인증
 * ({@code /admin/**} ADMIN)을 쓰고 응답은 v2 형식이다.
 */
@RestController
@RequestMapping("/admin/api/v2/users")
@RequiredArgsConstructor
public class MobileUserAdminController {

    private final MobileUserAdminService userAdminService;

    @GetMapping
    public MobileApiResponse<OffsetPage<MobileAdminUserResult>> list(
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) MobileUserStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(OffsetPage.MAX_SIZE) int size) {
        return MobileApiResponse.success(userAdminService.list(keyword, status, page, size));
    }

    @GetMapping("/{userId}")
    public MobileApiResponse<MobileAdminUserDetailResult> detail(@PathVariable Long userId) {
        return MobileApiResponse.success(userAdminService.detail(userId));
    }
}
