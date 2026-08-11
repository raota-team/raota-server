package com.raota.ramenshop.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record ShopStats(
        @Column(name = "view_count", nullable = false) int viewCount,
        @Column(name = "visit_count") int visitCount,
        @Column(name = "bookmark_count") int bookmarkCount
) {
    public static ShopStats init() {
        return new ShopStats(0, 0, 0);
    }

    public ShopStats increaseView() {
        return new ShopStats(viewCount + 1, visitCount, bookmarkCount);
    }

    public ShopStats increaseVisit() {
        return new ShopStats(viewCount, visitCount + 1, bookmarkCount);
    }

    public ShopStats increaseBookmark() {
        return new ShopStats(viewCount, visitCount, bookmarkCount + 1);
    }

    public ShopStats decreaseVisit() {
        if (visitCount <= 0) {
            return new ShopStats(viewCount, 0, bookmarkCount);
        }
        return new ShopStats(viewCount, visitCount - 1, bookmarkCount);
    }

    public ShopStats decreaseBookmark() {
        if (bookmarkCount <= 0) {
            return new ShopStats(viewCount, visitCount, 0);
        }
        return new ShopStats(viewCount, visitCount, bookmarkCount - 1);
    }
}
