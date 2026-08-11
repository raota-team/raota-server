package com.raota.account.presentation.admin.response;

import com.raota.account.domain.member.model.MemberActivityVisibility;

public record AdminUserActivityVisibilityResponse(
        boolean logsPublic,
        boolean visitsPublic,
        boolean postsPublic,
        boolean commentsPublic
) {
    public static AdminUserActivityVisibilityResponse from(MemberActivityVisibility visibility) {
        return new AdminUserActivityVisibilityResponse(
                visibility.isLogsPublic(),
                visibility.isVisitsPublic(),
                visibility.isPostsPublic(),
                visibility.isCommentsPublic()
        );
    }
}
