package com.raota.mobile.ramenlog.presentation;

import com.raota.mobile.common.cursor.CursorPage;
import com.raota.mobile.common.presentation.LoginUser;
import com.raota.mobile.common.presentation.response.MobileApiResponse;
import com.raota.mobile.ramenlog.application.result.MobileRamenLogCommentResult;
import com.raota.mobile.ramenlog.application.service.MobileRamenLogCommentService;
import com.raota.mobile.ramenlog.presentation.request.MobileCreateCommentRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 기록의 한 단계 댓글이다. 답글은 없다. */
@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class MobileRamenLogCommentController {

    private final MobileRamenLogCommentService comments;

    @GetMapping("/ramen-logs/{logId}/comments")
    public MobileApiResponse<CursorPage<MobileRamenLogCommentResult>> list(
            @LoginUser(required = false) Long userId,
            @PathVariable Long logId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(CursorPage.MAX_SIZE) int size) {
        return MobileApiResponse.success(comments.list(logId, userId, cursor, size));
    }

    @PostMapping("/ramen-logs/{logId}/comments")
    public ResponseEntity<MobileApiResponse<MobileRamenLogCommentResult>> create(
            @LoginUser Long userId,
            @PathVariable Long logId,
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9_-]+") String key,
            @Valid @RequestBody MobileCreateCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MobileApiResponse.success(comments.create(logId, userId, key, request.content())));
    }

    @DeleteMapping("/ramen-log-comments/{commentId}")
    public ResponseEntity<Void> delete(@LoginUser Long userId, @PathVariable Long commentId) {
        comments.delete(commentId, userId);
        return ResponseEntity.noContent().build();
    }
}
