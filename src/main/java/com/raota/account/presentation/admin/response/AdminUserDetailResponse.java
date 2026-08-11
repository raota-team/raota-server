package com.raota.account.presentation.admin.response;

import java.util.List;

public record AdminUserDetailResponse(
        Long id,
        AdminUserProfileResponse profile,
        List<AdminUserSocialAccountResponse> socialAccounts,
        AdminUserActivityStatsResponse activityStats,
        AdminUserActivityVisibilityResponse activityVisibility
) {
}
