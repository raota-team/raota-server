package com.raota.presentation.api.member.dto;


import com.raota.presentation.api.member.dto.UserStatsDto;

public record MyProfileResponse (
        Long user_id,
        String nickname,
        String profile_image_url,
        String background_image_url,
        String userDescription,
        UserStatsDto stats
){
}
