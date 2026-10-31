package com.raota.web.ramenlog.presentation.api;

import com.raota.global.presentation.common.ApiResponse;
import com.raota.global.presentation.common.PageResponse;
import com.raota.web.account.infrastructure.auth.LoginMember;
import com.raota.web.ramenlog.application.RamenProofPictureService;
import com.raota.web.ramenlog.presentation.api.contract.RamenProofPictureApi;
import com.raota.web.ramenlog.presentation.api.response.RamenShopProofPictureResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ramen-shops/{shopId}/photos")
@RequiredArgsConstructor
public class RamenProofPictureController implements RamenProofPictureApi {

    private final RamenProofPictureService proofPictureService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RamenShopProofPictureResponse>>> getProofPicture(
            @PathVariable Long shopId, @PageableDefault(size = 6, direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.success(PageResponse.from(proofPictureService.findProofPicture(shopId, pageable))));
    }

    @DeleteMapping("/{photoId}")
    public ResponseEntity<?> deleteProofPicture(@PathVariable Long photoId, @LoginMember Long memberId) {
        proofPictureService.deletePicture(photoId, memberId);
        return ResponseEntity.noContent().build();
    }
}
