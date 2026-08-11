package com.raota.account.presentation.member.response;

import com.raota.account.domain.member.model.MemberActivityVisibility;

public record ActivityVisibilityResponse(
        boolean logs,
        boolean visits,
        boolean posts,
        boolean comments
) {
    public static ActivityVisibilityResponse from(MemberActivityVisibility visibility) {
        if (visibility == null) {
            return allPublic();
        }
        return new ActivityVisibilityResponse(
                visibility.isLogsPublic(),
                visibility.isVisitsPublic(),
                visibility.isPostsPublic(),
                visibility.isCommentsPublic()
        );
    }

    public static ActivityVisibilityResponse allPublic() {
        return new ActivityVisibilityResponse(true, true, true, true);
    }
}
