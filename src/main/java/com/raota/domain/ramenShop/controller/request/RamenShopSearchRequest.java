package com.raota.domain.ramenShop.controller.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RamenShopSearchRequest {
    private String region;
    private String keyword;
    private String tag;
}
