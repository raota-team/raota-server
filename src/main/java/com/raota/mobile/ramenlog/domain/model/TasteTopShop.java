package com.raota.mobile.ramenlog.domain.model;

import java.time.LocalDate;

/** 매장 이름과 사진은 굳히지 않고 응답할 때 읽는다. */
public record TasteTopShop(Long shopId, int visitCount, String topMenu, String topRamenType, LocalDate lastVisitedAt) {}
