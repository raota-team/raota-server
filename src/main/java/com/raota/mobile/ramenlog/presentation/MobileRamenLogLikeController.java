package com.raota.mobile.ramenlog.presentation;

import com.raota.mobile.common.presentation.LoginUser;
import com.raota.mobile.common.presentation.response.MobileApiResponse;
import com.raota.mobile.ramenlog.application.result.MobileRamenLogLikeResult;
import com.raota.mobile.ramenlog.application.service.MobileRamenLogLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 이미 공감했거나 공감하지 않은 상태에서 다시 요청해도 성공한다. */
@RestController
@RequestMapping("/api/v2/ramen-logs")
@RequiredArgsConstructor
public class MobileRamenLogLikeController {

    private final MobileRamenLogLikeService likes;

    @PutMapping("/{logId}/like")
    public MobileApiResponse<MobileRamenLogLikeResult> like(@LoginUser Long userId, @PathVariable Long logId) {
        return MobileApiResponse.success(likes.like(logId, userId));
    }

    @DeleteMapping("/{logId}/like")
    public MobileApiResponse<MobileRamenLogLikeResult> unlike(@LoginUser Long userId, @PathVariable Long logId) {
        return MobileApiResponse.success(likes.unlike(logId, userId));
    }

}
