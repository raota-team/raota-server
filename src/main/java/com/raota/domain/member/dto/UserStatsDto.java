package com.raota.domain.member.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class UserStatsDto {
    private int visited_restaurant_count;
    private int total_photo_count;
    private int total_bookmark_count;
    private int post_count;
    private int comment_count;
}
