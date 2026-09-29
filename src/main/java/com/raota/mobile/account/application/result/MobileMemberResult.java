package com.raota.mobile.account.application.result;

import com.raota.mobile.account.domain.model.MobileUser;

/** 로그인과 회원 조회에 쓰는 모바일 회원 프로필이다. */
public record MobileMemberResult(String id, String email, String nickname, String avatarUrl, String bio,
        String favoriteRamenType, String status) {

    public static MobileMemberResult from(MobileUser user) {
        return new MobileMemberResult(String.valueOf(user.getId()), user.getEmail(), user.getNickname(),
                user.getAvatarUrl(), user.getBio(), user.getFavoriteRamenType(), user.getStatus().name());
    }

}
