package com.raota.mobile.shop.application.command;

import com.raota.mobile.shop.domain.model.MobileShopProfile;
import java.util.List;

/** 관리자 매장 생성·수정 입력이다. {@code imageUrls}는 순서대로 저장하고 첫 장이 대표 사진이다. */
public record MobileAdminShopCommand(MobileShopProfile profile, List<String> imageUrls) {

    public MobileAdminShopCommand {
        imageUrls = List.copyOf(imageUrls);
    }

}
