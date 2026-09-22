package com.raota.mobile.common.presentation;

import com.raota.global.presentation.common.RequestIdFilter;
import com.raota.global.presentation.v2.V2ApiResponse;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 앱이 서버 연결과 v2 응답 형식을 확인하는 용도의 공개 엔드포인트.
 */
@RestController
@RequestMapping("/api/v2")
public class PingController {

    @GetMapping("/ping")
    public V2ApiResponse<PingResponse> ping() {
        return V2ApiResponse.success(
                new PingResponse("ok", Instant.now()),
                RequestIdFilter.currentRequestId()
        );
    }

    public record PingResponse(String status, Instant serverTime) {
    }
}
