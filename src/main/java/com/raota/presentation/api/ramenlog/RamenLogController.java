package com.raota.presentation.api.ramenlog;

import com.raota.application.ramenlog.RamenLogService;
import com.raota.infrastructure.auth.LoginMember;
import com.raota.presentation.api.ramenlog.request.RamenLogSort;
import com.raota.presentation.api.ramenlog.request.RamenLogUpsertRequest;
import com.raota.presentation.api.ramenlog.response.RamenLogLikeResponse;
import com.raota.presentation.api.ramenlog.response.RamenLogResponse;
import com.raota.presentation.api.ramenlog.response.RamenLogShopResponse;
import com.raota.presentation.common.ApiResponse;
import com.raota.presentation.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "라멘로그", description = "라멘 취향 기록 API")
@RestController
@RequiredArgsConstructor
@RequestMapping
public class RamenLogController {

    private final RamenLogService ramenLogService;

    @Operation(summary = "공개 라멘로그 목록 조회")
    @GetMapping("/ramen-logs")
    public ResponseEntity<ApiResponse<PageResponse<RamenLogResponse>>> getPublicLogs(
            @LoginMember(required = false) Long memberId,
            @RequestParam(defaultValue = "LATEST") RamenLogSort sort,
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 8, direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(
                ramenLogService.getPublicLogs(memberId, sort, shopId, keyword, pageable)
        )));
    }

    @Operation(summary = "라멘로그 상세 조회")
    @GetMapping("/ramen-logs/{logId}")
    public ResponseEntity<ApiResponse<RamenLogResponse>> getLog(
            @PathVariable Long logId,
            @LoginMember(required = false) Long memberId
    ) {
        return ResponseEntity.ok(ApiResponse.success(ramenLogService.getLog(logId, memberId)));
    }

    @Operation(summary = "라멘로그 작성")
    @PostMapping("/ramen-logs")
    public ResponseEntity<ApiResponse<RamenLogResponse>> create(
            @Valid @RequestBody RamenLogUpsertRequest request,
            @LoginMember Long memberId
    ) {
        return ResponseEntity.ok(ApiResponse.success(ramenLogService.create(request, memberId)));
    }

    @Operation(summary = "라멘로그 수정")
    @PatchMapping("/ramen-logs/{logId}")
    public ResponseEntity<ApiResponse<RamenLogResponse>> update(
            @PathVariable Long logId,
            @Valid @RequestBody RamenLogUpsertRequest request,
            @LoginMember Long memberId
    ) {
        return ResponseEntity.ok(ApiResponse.success(ramenLogService.update(logId, request, memberId)));
    }

    @Operation(summary = "라멘로그 삭제")
    @DeleteMapping("/ramen-logs/{logId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long logId,
            @LoginMember Long memberId
    ) {
        ramenLogService.delete(logId, memberId);
        return ResponseEntity.ok(ApiResponse.success("라멘로그가 삭제되었습니다.", null));
    }

    @Operation(summary = "라멘로그 좋아요 토글")
    @PostMapping("/ramen-logs/{logId}/likes")
    public ResponseEntity<ApiResponse<RamenLogLikeResponse>> toggleLike(
            @PathVariable Long logId,
            @LoginMember Long memberId
    ) {
        return ResponseEntity.ok(ApiResponse.success(ramenLogService.toggleLike(logId, memberId)));
    }

    @Operation(summary = "내 라멘로그 목록 조회")
    @GetMapping("/users/me/ramen-logs")
    public ResponseEntity<ApiResponse<PageResponse<RamenLogResponse>>> getMyLogs(
            @LoginMember Long memberId,
            @RequestParam(required = false) Long shopId,
            @PageableDefault(size = 8) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(
                ramenLogService.getMemberLogs(memberId, memberId, true, shopId, pageable)
        )));
    }

    @Operation(summary = "사용자 공개 라멘로그 목록 조회")
    @GetMapping("/users/{userId}/ramen-logs")
    public ResponseEntity<ApiResponse<PageResponse<RamenLogResponse>>> getUserLogs(
            @PathVariable Long userId,
            @LoginMember(required = false) Long viewerId,
            @RequestParam(required = false) Long shopId,
            @PageableDefault(size = 8) Pageable pageable
    ) {
        boolean owner = viewerId != null && viewerId.equals(userId);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(
                ramenLogService.getMemberLogs(userId, viewerId, owner, shopId, pageable)
        )));
    }

    @Operation(summary = "내 라멘로그 가게 필터 목록 조회")
    @GetMapping("/users/me/ramen-logs/shops")
    public ResponseEntity<ApiResponse<List<RamenLogShopResponse>>> getMyLogShops(
            @LoginMember Long memberId
    ) {
        return ResponseEntity.ok(ApiResponse.success(ramenLogService.getMemberLogShops(memberId, true)));
    }

    @Operation(summary = "사용자 공개 라멘로그 가게 필터 목록 조회")
    @GetMapping("/users/{userId}/ramen-logs/shops")
    public ResponseEntity<ApiResponse<List<RamenLogShopResponse>>> getUserLogShops(
            @PathVariable Long userId,
            @LoginMember(required = false) Long viewerId
    ) {
        boolean owner = viewerId != null && viewerId.equals(userId);
        return ResponseEntity.ok(ApiResponse.success(ramenLogService.getMemberLogShops(userId, owner)));
    }
}
