package com.raota.agent.presentation.mobile;

import com.raota.agent.application.mobile.MobileCurationService;
import com.raota.agent.application.mobile.result.MobileCurationResult;
import com.raota.mobile.common.presentation.LoginUser;
import com.raota.mobile.common.presentation.response.MobileApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 홈의 오늘의 큐레이션이다. 고른 매장이 없으면 {@code data: null}이다. */
@RestController
@RequestMapping("/api/v2/curations")
@RequiredArgsConstructor
public class MobileCurationController {

    private final MobileCurationService curations;

    @GetMapping("/today")
    public MobileApiResponse<MobileCurationResult> today(@LoginUser(required = false) Long userId) {
        return MobileApiResponse.success(curations.today(userId).orElse(null));
    }

}
