package com.raota.domain.ramenShop.controller;

import com.raota.domain.ramenShop.controller.response.VotingStatusResponse;
import com.raota.domain.ramenShop.controller.contract.MenuVoteApi;
import com.raota.domain.ramenShop.service.MenuVoteService;
import com.raota.global.auth.LoginMember;
import com.raota.global.common.ApiResponse;
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

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<VotingStatusResponse>> getVoteStatus(@PathVariable Long shopId) {
        VotingStatusResponse response = menuVoteService.getVotingStatus(shopId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    @PostMapping("/menus/{menuId}")
    public ResponseEntity<ApiResponse<VotingStatusResponse>> votingMenu(
            @PathVariable Long shopId,
            @PathVariable Long menuId,
            @LoginMember Long memberId) {
        VotingStatusResponse response = menuVoteService.voteTheMenu(shopId, menuId,memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
