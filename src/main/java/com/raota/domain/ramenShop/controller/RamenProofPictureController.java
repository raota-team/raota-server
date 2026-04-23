package com.raota.domain.ramenShop.controller;

import com.raota.domain.ramenShop.controller.contract.RamenProofPictureApi;
import com.raota.domain.ramenShop.controller.response.ProofPictureInfoResponse;
import com.raota.domain.ramenShop.controller.response.RamenShopProofPictureResponse;
import com.raota.global.auth.LoginMember;
import com.raota.global.common.ApiResponse;
import com.raota.global.common.PageResponse;
import com.raota.domain.ramenShop.service.RamenProofPictureService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.raota.domain.ramenShop.controller.request.ProofPictureUploadRequest;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/ramen-shops/{shopId}/photos")
@RequiredArgsConstructor
public class RamenProofPictureController implements RamenProofPictureApi {

    private final RamenProofPictureService proofPictureService;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<ProofPictureInfoResponse>> addProofPicture(
            @PathVariable Long shopId,
            @RequestBody ProofPictureUploadRequest request,
            @LoginMember Long memberId
    ) {
        ProofPictureInfoResponse response = proofPictureService.addProofPicture(
                shopId, 
                request.getImageUrl(),
                request.getImageName(),
                request.getDescription(), 
                memberId
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RamenShopProofPictureResponse>>> getProofPicture(
            @PathVariable Long shopId,
            @PageableDefault(size = 6, direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(proofPictureService.findProofPicture(shopId,pageable))));
    }
}
