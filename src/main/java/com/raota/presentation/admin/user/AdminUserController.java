package com.raota.presentation.admin.user;

import com.raota.application.admin.user.AdminUserService;
import com.raota.domain.auth.model.AuthProvider;
import com.raota.presentation.admin.user.response.AdminUserDetailResponse;
import com.raota.presentation.admin.user.response.AdminUserListItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public String users(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean registrationCompleted,
            @RequestParam(required = false) Boolean deleted,
            @RequestParam(required = false) AuthProvider provider,
            @RequestParam(required = false) Boolean emailPresent,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            Model model
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

        model.addAttribute("users", users);
        model.addAttribute("keyword", keyword);
        model.addAttribute("registrationCompleted", registrationCompleted);
        model.addAttribute("deleted", deleted);
        model.addAttribute("provider", provider);
        model.addAttribute("emailPresent", emailPresent);
        model.addAttribute("providers", AuthProvider.values());
        return "admin/users";
    }

    @GetMapping("/{memberId}")
    public String userDetail(@PathVariable Long memberId, Model model) {
        AdminUserDetailResponse user = adminUserService.getUser(memberId);
        model.addAttribute("user", user);
        return "admin/user-detail";
    }
}
