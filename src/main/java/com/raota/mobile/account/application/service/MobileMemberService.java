package com.raota.mobile.account.application.service;

import com.raota.mobile.account.application.result.MobileMemberResult;
import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.model.MobileUserStatus;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 인증된 모바일 회원의 프로필을 조회하고 수정한다. */
@Service
@RequiredArgsConstructor
public class MobileMemberService {

    private final MobileUserRepository users;

    @Transactional(readOnly = true)
    public MobileMemberResult me(Long userId) {
        return MobileMemberResult.from(availableUser(userId));
    }

    @Transactional
    public MobileMemberResult updateProfile(Long userId, String email, String avatarUrl, String bio,
            String favoriteRamenType) {
        MobileUser user = availableUser(userId);
        user.updateProfile(email, avatarUrl, bio, favoriteRamenType);
        return MobileMemberResult.from(user);
    }

    private MobileUser availableUser(Long userId) {
        MobileUser user = users.findById(userId)
            .orElseThrow(() -> new MobileException(MobileErrorCode.UNAUTHORIZED, "사용할 수 없는 계정입니다."));
        if (user.getStatus() != MobileUserStatus.ONBOARDING && user.getStatus() != MobileUserStatus.ACTIVE) {
            throw new MobileException(MobileErrorCode.UNAUTHORIZED, "사용할 수 없는 계정입니다.");
        }
        return user;
    }

}
