package com.raota.presentation.admin.ramenShop;

import com.raota.presentation.admin.ramenShop.request.RamenShopAdminForm;
import com.raota.presentation.admin.ramenShop.request.RamenShopVisibilityBulkUpdateRequest;
import com.raota.presentation.admin.ramenShop.request.RamenShopVisibilityUpdateRequest;
import com.raota.presentation.admin.ramenShop.response.RamenShopAdminMutationResponse;
import com.raota.presentation.admin.ramenShop.response.RamenShopAdminSummaryResponse;
import com.raota.presentation.admin.ramenShop.response.RamenShopVisibilityBulkUpdateResponse;
import com.raota.application.admin.ramenShop.RamenShopAdminService;
import com.raota.presentation.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/api/ramen-shops")
@RequiredArgsConstructor
public class RamenShopAdminApiController {

    private final RamenShopAdminService ramenShopAdminService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RamenShopAdminSummaryResponse>>> getShops() {
        return ResponseEntity.ok(ApiResponse.success(ramenShopAdminService.getShopSummaries()));
    }

    @GetMapping("/{shopId}")
    public ResponseEntity<ApiResponse<RamenShopAdminForm>> getShop(@PathVariable Long shopId) {
        return ResponseEntity.ok(ApiResponse.success(ramenShopAdminService.getForm(shopId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RamenShopAdminMutationResponse>> createShop(
            @Valid @RequestBody RamenShopAdminForm form,
            BindingResult bindingResult
    ) {
        rejectIfInvalid(bindingResult);
        Long shopId = ramenShopAdminService.createShop(form);
        return ResponseEntity.ok(ApiResponse.success("라멘집이 추가되었습니다.", new RamenShopAdminMutationResponse(shopId)));
    }

    @PatchMapping("/{shopId}")
    public ResponseEntity<ApiResponse<RamenShopAdminMutationResponse>> updateShop(
            @PathVariable Long shopId,
            @Valid @RequestBody RamenShopAdminForm form,
            BindingResult bindingResult
    ) {
        rejectIfInvalid(bindingResult);
        ramenShopAdminService.updateShop(shopId, form);
        return ResponseEntity.ok(ApiResponse.success("라멘집 정보가 수정되었습니다.", new RamenShopAdminMutationResponse(shopId)));
    }

    @PatchMapping("/{shopId}/visibility")
    public ResponseEntity<ApiResponse<RamenShopAdminMutationResponse>> updateVisibility(
            @PathVariable Long shopId,
            @Valid @RequestBody RamenShopVisibilityUpdateRequest request
    ) {
        ramenShopAdminService.updateVisibility(shopId, request.published());
        return ResponseEntity.ok(ApiResponse.success(
                request.published() ? "라멘집이 공개되었습니다." : "라멘집이 숨김 처리되었습니다.",
                new RamenShopAdminMutationResponse(shopId)
        ));
    }

    @PatchMapping("/visibility")
    public ResponseEntity<ApiResponse<RamenShopVisibilityBulkUpdateResponse>> updateVisibility(
            @Valid @RequestBody RamenShopVisibilityBulkUpdateRequest request
    ) {
        int updatedCount = ramenShopAdminService.updateVisibility(
                request.fromId(),
                request.toId(),
                request.published()
        );
        return ResponseEntity.ok(ApiResponse.success(
                "라멘집 공개 상태가 일괄 변경되었습니다.",
                new RamenShopVisibilityBulkUpdateResponse(
                        request.fromId(),
                        request.toId(),
                        request.published(),
                        updatedCount
                )
        ));
    }

    @DeleteMapping("/{shopId}")
    public ResponseEntity<ApiResponse<RamenShopAdminMutationResponse>> deleteShop(@PathVariable Long shopId) {
        ramenShopAdminService.deleteShop(shopId);
        return ResponseEntity.ok(ApiResponse.success(
                "라멘집이 삭제되었습니다.",
                new RamenShopAdminMutationResponse(shopId)
        ));
    }

    private void rejectIfInvalid(BindingResult bindingResult) {
        if (!bindingResult.hasErrors()) {
            return;
        }
        throw new IllegalArgumentException(bindingResult.getFieldErrors().stream()
                .findFirst()
                .map(this::formatFieldError)
                .orElse("입력값이 올바르지 않습니다."));
    }

    private String formatFieldError(FieldError error) {
        return "%s: %s".formatted(error.getField(), error.getDefaultMessage());
    }
}
