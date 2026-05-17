package com.raota.presentation.api.ramenShop;

import com.raota.presentation.api.ramenShop.contract.RamenProofPictureApi;
import com.raota.presentation.api.ramenShop.dto.ProofPictureInfoResponse;
import com.raota.presentation.api.ramenShop.dto.RamenShopProofPictureResponse;
import com.raota.infrastructure.auth.LoginMember;
import com.raota.presentation.common.ApiResponse;
import com.raota.presentation.common.PageResponse;
import com.raota.application.ramenShop.RamenProofPictureService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.raota.presentation.api.ramenShop.dto.ProofPictureUploadRequest;
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
                request.getDescription(),
                request.getMenuName(),
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

    @DeleteMapping("/{photoId}")
    public ResponseEntity<?> deleteProofPicture(
            @PathVariable Long photoId,
            @LoginMember Long memberId){
        proofPictureService.deletePicture(photoId,memberId);
        return ResponseEntity.noContent().build();
    }

}
