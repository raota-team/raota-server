package com.raota.presentation.api.member.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class UserStatsDto {
    private Long visited_restaurant_count;
    private Long total_photo_count;
    private Long total_bookmark_count;
    private Long post_count;
    private Long comment_count;
}
