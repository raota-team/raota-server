package com.raota.account.presentation.admin.response;

import com.raota.account.domain.member.model.MemberActivityStats;

public record AdminUserActivityStatsResponse(
        int visitedRestaurantCount,
        int photoCount,
        int bookmarkCount,
        int postCount,
        int commentCount
) {
    public static AdminUserActivityStatsResponse from(MemberActivityStats stats) {
        return new AdminUserActivityStatsResponse(
                stats.visitedRestaurantCount(),
                stats.photoCount(),
                stats.bookmarkCount(),
                stats.postCount(),
                stats.commentCount()
        );
    }
}
