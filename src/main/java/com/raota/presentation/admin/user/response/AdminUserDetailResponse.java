package com.raota.presentation.admin.user.response;

import java.util.List;

public record AdminUserDetailResponse(
        AdminUserProfileResponse profile,
        List<AdminUserSocialAccountResponse> socialAccounts,
        AdminUserActivityStatsResponse activityStats,
        AdminUserActivityVisibilityResponse activityVisibility
) {
}
