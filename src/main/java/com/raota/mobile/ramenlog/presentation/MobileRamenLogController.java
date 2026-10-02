package com.raota.mobile.ramenlog.presentation;

import com.raota.mobile.common.presentation.LoginUser;
import com.raota.mobile.common.presentation.response.MobileApiResponse;
import com.raota.mobile.ramenlog.application.result.MobileRamenLogDetail;
import com.raota.mobile.ramenlog.application.service.MobileRamenLogService;
import com.raota.mobile.ramenlog.presentation.request.MobileCreateRamenLogRequest;
import com.raota.mobile.ramenlog.presentation.request.MobileUpdateRamenLogRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/ramen-logs")
@RequiredArgsConstructor
public class MobileRamenLogController {

    private final MobileRamenLogService logs;

    @PostMapping
    public ResponseEntity<MobileApiResponse<MobileRamenLogDetail>> create(@LoginUser Long userId,
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9_-]+") String key,
            @Valid @RequestBody MobileCreateRamenLogRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(MobileApiResponse.success(logs.create(userId, key, request.toCommand())));
    }

    @GetMapping("/{logId:[0-9]+}")
    public MobileApiResponse<MobileRamenLogDetail> detail(@LoginUser(required = false) Long userId,
            @PathVariable Long logId) {
        return MobileApiResponse.success(logs.detail(logId, userId));
    }

    @PatchMapping("/{logId:[0-9]+}")
    public MobileApiResponse<MobileRamenLogDetail> update(@LoginUser Long userId, @PathVariable Long logId,
            @Valid @RequestBody MobileUpdateRamenLogRequest request) {
        return MobileApiResponse.success(logs.update(logId, userId, request.toCommand()));
    }

    @DeleteMapping("/{logId:[0-9]+}")
    public ResponseEntity<Void> delete(@LoginUser Long userId, @PathVariable Long logId) {
        logs.delete(logId, userId);
        return ResponseEntity.noContent().build();
    }

}
