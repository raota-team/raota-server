package com.raota.domain.member.controller.response;


import com.raota.domain.member.dto.UserStatsDto;

public record MyProfileResponse (
        Long user_id,
        String nickname,
        String profile_image_url,
        String userDescription,
        UserStatsDto stats
){
}
