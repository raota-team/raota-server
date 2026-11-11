package com.raota.mobile.notification.presentation;

import com.raota.mobile.common.presentation.LoginUser;
import com.raota.mobile.notification.application.service.MobilePushTokenService;
import com.raota.mobile.notification.domain.model.PushLanguage;
import com.raota.mobile.notification.presentation.request.MobilePushTokenRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/members/me/push-tokens")
@RequiredArgsConstructor
public class MobilePushTokenController {

    private final MobilePushTokenService tokens;

    /** 알림 언어는 이 요청의 {@code Accept-Language}로 정한다. 앱이 실행할 때마다 다시 등록해 언어 변경이 반영된다. */
    @PostMapping
    public ResponseEntity<Void> register(
            @LoginUser Long userId,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage,
            @Valid @RequestBody MobilePushTokenRequest request) {
        tokens.register(userId, request.token(), request.platform(), PushLanguage.fromAcceptLanguage(acceptLanguage));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{token}")
    public ResponseEntity<Void> unregister(@LoginUser Long userId, @PathVariable String token) {
        tokens.unregister(userId, token);
        return ResponseEntity.noContent().build();
    }
}
