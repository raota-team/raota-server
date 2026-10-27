package com.raota.mobile.ramenlog.presentation;

import com.raota.mobile.common.presentation.LoginUser;
import com.raota.mobile.common.presentation.response.MobileApiResponse;
import com.raota.mobile.ramenlog.application.result.MobileBlockedMemberResult;
import com.raota.mobile.ramenlog.application.service.MobileContentReportService;
import com.raota.mobile.ramenlog.application.service.MobileUserBlockService;
import com.raota.mobile.ramenlog.presentation.request.MobileReportRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 라운지의 신고와 회원 차단이다(App Store 심사 가이드라인 1.2). */
@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class MobileLoungeSafetyController {

    private final MobileContentReportService reports;

    private final MobileUserBlockService blocks;

    @PostMapping("/reports")
    public ResponseEntity<Void> report(@LoginUser Long userId, @Valid @RequestBody MobileReportRequest request) {
        reports.report(userId, request.toCommand());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/members/{memberId}/block")
    public ResponseEntity<Void> block(@LoginUser Long userId, @PathVariable Long memberId) {
        blocks.block(userId, memberId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/members/{memberId}/block")
    public ResponseEntity<Void> unblock(@LoginUser Long userId, @PathVariable Long memberId) {
        blocks.unblock(userId, memberId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/members/me/blocks")
    public MobileApiResponse<List<MobileBlockedMemberResult>> blocked(@LoginUser Long userId) {
        return MobileApiResponse.success(blocks.blocked(userId));
    }

}
