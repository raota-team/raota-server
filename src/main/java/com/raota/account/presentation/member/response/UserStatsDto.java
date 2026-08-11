package com.raota.account.presentation.member.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class UserStatsDto {
    private Long visited_restaurant_count;
    private Long total_photo_count;
    private Long total_log_count;
    private Long total_bookmark_count;
    private Long post_count;
    private Long comment_count;

    public UserStatsDto maskPrivate(ActivityVisibilityResponse visibility) {
        return new UserStatsDto(
                visibility.visits() ? visited_restaurant_count : null,
                visibility.logs() ? total_photo_count : null,
                visibility.logs() ? total_log_count : null,
                total_bookmark_count,
                visibility.posts() ? post_count : null,
                visibility.comments() ? comment_count : null
        );
    }
}
