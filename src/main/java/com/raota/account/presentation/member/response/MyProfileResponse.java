package com.raota.account.presentation.member.response;


import com.raota.account.presentation.member.response.UserStatsDto;

public record MyProfileResponse (
        Long user_id,
        String nickname,
        String email,
        String profile_image_url,
        String background_image_url,
        String userDescription,
        UserStatsDto stats,
        ActivityVisibilityResponse activity_visibility
){
    public MyProfileResponse maskPrivateActivityStats() {
        ActivityVisibilityResponse visibility = activity_visibility == null
                ? ActivityVisibilityResponse.allPublic()
                : activity_visibility;
        return new MyProfileResponse(
                user_id,
                nickname,
                null,
                profile_image_url,
                background_image_url,
                userDescription,
                stats.maskPrivate(visibility),
                visibility
        );
    }
}
