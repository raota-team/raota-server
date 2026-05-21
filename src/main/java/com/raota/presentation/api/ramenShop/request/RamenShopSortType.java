package com.raota.presentation.api.ramenShop.request;

import org.springframework.data.domain.Sort;

public enum RamenShopSortType {
    LATEST(Sort.by(Sort.Order.desc("id"))),
    POPULAR(Sort.by(
            Sort.Order.desc("stats.bookmarkCount"),
            Sort.Order.desc("stats.visitCount"),
            Sort.Order.desc("id")
    )),
    NAME(Sort.by(
            Sort.Order.asc("name"),
            Sort.Order.desc("id")
    )),
    VISITS(Sort.by(
            Sort.Order.desc("stats.visitCount"),
            Sort.Order.desc("id")
    ));

    private final Sort sort;

    RamenShopSortType(Sort sort) {
        this.sort = sort;
    }

    public Sort toSort() {
        return sort;
    }

    public static RamenShopSortType defaultIfNull(RamenShopSortType sortType) {
        return sortType == null ? LATEST : sortType;
    }
}
