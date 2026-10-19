package com.raota.mobile.ramenlog.presentation.admin;

import com.raota.mobile.common.copy.LegacyCopyReport;
import com.raota.mobile.common.presentation.response.MobileApiResponse;
import com.raota.mobile.ramenlog.application.service.MobileRamenLogCopyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * v1 라멘 기록·사진·공감을 v2로 복사하는 관리자 API다(#59). 웹 관리자 인증({@code /admin/**} ADMIN)을 쓰고 응답은 v2
 * 형식의 대조표다. 회원 복사({@code /members}) 뒤에 실행하며, 다시 실행해도 이미 복사한 행은 건너뛴다.
 */
@RestController
@RequestMapping("/admin/api/v2/copy")
@RequiredArgsConstructor
public class MobileRamenLogCopyAdminController {

    private final MobileRamenLogCopyService logs;

    @PostMapping("/ramen-logs")
    public MobileApiResponse<LegacyCopyReport> copyRamenLogs() {
        return MobileApiResponse.success(logs.copy());
    }

}
