package com.raota.presentation.api.ramenShop.response;

import com.raota.domain.ramenShop.model.EventMenu;
import com.raota.domain.ramenShop.model.NormalMenu;
import com.raota.domain.ramenShop.model.RamenShop;
import java.util.List;

public record RamenShopMenuOptionsResponse(
        Long shopId,
        String shopName,
        List<MenuOptionResponse> normalMenus,
        List<MenuOptionResponse> eventMenus
) {

    public static RamenShopMenuOptionsResponse from(
            RamenShop ramenShop,
            List<NormalMenu> normalMenus,
            List<EventMenu> eventMenus
    ) {
        return new RamenShopMenuOptionsResponse(
                ramenShop.getId(),
                ramenShop.getName(),
                normalMenus.stream()
                        .map(menu -> new MenuOptionResponse(menu.getId(), menu.getName()))
                        .toList(),
                eventMenus.stream()
                        .map(menu -> new MenuOptionResponse(menu.getId(), menu.getName()))
                        .toList()
        );
    }

    public record MenuOptionResponse(
            Long id,
            String name
    ) {
    }
}
