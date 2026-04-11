package com.raota.domain.member.model;

import jakarta.persistence.Embeddable;

@Embeddable
public record MemberActivityStats(
        int visitedRestaurantCount,
        int photoCount,
        int bookmarkCount,
        int postCount,
        int commentCount
) {
    public static MemberActivityStats init() {
        return new MemberActivityStats(0, 0, 0, 0, 0);
    }

    public MemberActivityStats increaseVisited() {
        return new MemberActivityStats(visitedRestaurantCount + 1, photoCount, bookmarkCount, postCount, commentCount);
    }

    public MemberActivityStats increasePhoto() {
        return new MemberActivityStats(visitedRestaurantCount, photoCount + 1, bookmarkCount, postCount, commentCount);
    }

    public MemberActivityStats increaseBookmark() {
        return new MemberActivityStats(visitedRestaurantCount, photoCount, bookmarkCount + 1, postCount, commentCount);
    }

    public MemberActivityStats increasePost() {
        return new MemberActivityStats(visitedRestaurantCount, photoCount, bookmarkCount, postCount + 1, commentCount);
    }

    public MemberActivityStats increaseComment() {
        return new MemberActivityStats(visitedRestaurantCount, photoCount, bookmarkCount, postCount, commentCount + 1);
    }

    public MemberActivityStats decreaseVisit() {
        return new MemberActivityStats(Math.max(0, visitedRestaurantCount - 1), photoCount, bookmarkCount, postCount, commentCount);
    }

    public MemberActivityStats decreasePhoto() {
        return new MemberActivityStats(visitedRestaurantCount, Math.max(0, photoCount - 1), bookmarkCount, postCount, commentCount);
    }

    public MemberActivityStats decreaseBookmark() {
        return new MemberActivityStats(visitedRestaurantCount, photoCount, Math.max(0, bookmarkCount - 1), postCount, commentCount);
    }

    public MemberActivityStats decreasePost() {
        return new MemberActivityStats(visitedRestaurantCount, photoCount, bookmarkCount, Math.max(0, postCount - 1), commentCount);
    }

    public MemberActivityStats decreaseComment() {
        return new MemberActivityStats(visitedRestaurantCount, photoCount, bookmarkCount, postCount, Math.max(0, commentCount - 1));
    }
}
