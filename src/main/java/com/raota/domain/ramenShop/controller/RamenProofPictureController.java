package com.raota.domain.ramenShop.controller;

import com.raota.domain.proofPicture.controller.contract.RamenProofPictureApi;
import com.raota.domain.proofPicture.controller.response.ProofPictureInfoResponse;
import com.raota.domain.proofPicture.controller.response.RamenShopProofPictureResponse;
import com.raota.global.auth.LoginMember;
import com.raota.global.common.ApiResponse;
import com.raota.domain.proofPicture.service.RamenProofPictureService;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/ramen-shops/{shopId}/photos")
@RequiredArgsConstructor
public class RamenProofPictureController implements RamenProofPictureApi {

    private final RamenProofPictureService proofPictureService;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<ProofPictureInfoResponse>> addProofPicture(
            @PathVariable Long shopId,
            @RequestPart("file") MultipartFile file,
            @LoginMember Long memberId
    ) {
        ProofPictureInfoResponse response = proofPictureService.addProofPicture(shopId,file,memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<Page<RamenShopProofPictureResponse>>> getProofPicture(
            @PathVariable Long shopId,
            @PageableDefault(size = 6, direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(proofPictureService.findProofPicture(shopId,pageable)));
    }
}
