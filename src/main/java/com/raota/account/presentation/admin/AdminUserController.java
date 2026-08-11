package com.raota.account.presentation.admin;

import com.raota.account.application.admin.AdminUserService;
import com.raota.account.domain.auth.model.AuthProvider;
import com.raota.account.presentation.admin.response.AdminUserDetailResponse;
import com.raota.account.presentation.admin.response.AdminUserListItemResponse;
import com.raota.global.presentation.common.ApiResponse;
import com.raota.global.presentation.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/api/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminUserListItemResponse>>> users(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean registrationCompleted,
            @RequestParam(required = false) Boolean deleted,
            @RequestParam(required = false) AuthProvider provider,
            @RequestParam(required = false) Boolean emailPresent,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        Page<AdminUserListItemResponse> users = adminUserService.getUsers(
                keyword,
                registrationCompleted,
                deleted,
                provider,
                emailPresent,
                page,
                size
        );
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(users)));
    }

    @GetMapping("/{memberId}")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> userDetail(@PathVariable Long memberId) {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.getUser(memberId)));
    }
}
