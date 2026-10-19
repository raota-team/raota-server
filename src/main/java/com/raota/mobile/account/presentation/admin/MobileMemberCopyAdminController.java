package com.raota.mobile.account.presentation.admin;

import com.raota.mobile.account.application.service.MobileMemberCopyService;
import com.raota.mobile.common.copy.LegacyCopyReport;
import com.raota.mobile.common.presentation.response.MobileApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * v1 회원·소셜 계정을 v2로 복사하는 관리자 API다(#59). 웹 관리자 인증({@code /admin/**} ADMIN)을 쓰고 응답은 v2 형식의
 * 대조표다. 다시 실행해도 이미 복사한 행은 건너뛴다. 기록({@code /ramen-logs})·북마크({@code /bookmarks})보다 먼저
 * 실행한다.
 */
@RestController
@RequestMapping("/admin/api/v2/copy")
@RequiredArgsConstructor
public class MobileMemberCopyAdminController {

    private final MobileMemberCopyService members;

    @PostMapping("/members")
    public MobileApiResponse<LegacyCopyReport> copyMembers() {
        return MobileApiResponse.success(members.copy());
    }

}
