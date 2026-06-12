package com.raota.presentation.api.ramenShop;

import com.raota.presentation.api.ramenShop.response.VotingStatusResponse;
import com.raota.presentation.api.ramenShop.contract.MenuVoteApi;
import com.raota.application.ramenShop.MenuVoteService;
import com.raota.infrastructure.auth.AnonymousVoteCookieManager;
import com.raota.infrastructure.auth.LoginMember;
import com.raota.presentation.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/ramen-shops/{shopId}/votes")
public class MenuVoteController implements MenuVoteApi {

    private final MenuVoteService menuVoteService;
    private final AnonymousVoteCookieManager anonymousVoteCookieManager;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<VotingStatusResponse>> getVoteStatus(
            @PathVariable Long shopId, 
            @LoginMember(required = false) Long memberId,
            HttpServletRequest request) {
        String anonymousVoteId = anonymousVoteCookieManager.extractAnonymousVoteId(request);
        VotingStatusResponse response = menuVoteService.getVotingStatus(shopId, memberId, anonymousVoteId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    @PostMapping("/menus/{menuId}")
    public ResponseEntity<ApiResponse<VotingStatusResponse>> votingMenu(
            @PathVariable Long shopId,
            @PathVariable Long menuId,
            @LoginMember(required = false) Long memberId,
            HttpServletRequest request,
            HttpServletResponse servletResponse) {
        String anonymousVoteId = null;
        if (memberId == null) {
            anonymousVoteId = anonymousVoteCookieManager.extractAnonymousVoteId(request);
            if (anonymousVoteId == null || anonymousVoteId.isBlank()) {
                anonymousVoteId = anonymousVoteCookieManager.createAnonymousVoteId();
                servletResponse.addHeader(
                        "Set-Cookie",
                        anonymousVoteCookieManager.createAnonymousVoteCookie(anonymousVoteId).toString()
                );
            }
        }

        VotingStatusResponse response = menuVoteService.voteTheMenu(shopId, menuId, memberId, anonymousVoteId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
