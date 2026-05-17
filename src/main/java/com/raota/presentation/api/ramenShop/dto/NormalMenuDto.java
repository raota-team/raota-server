package com.raota.presentation.api.ramenShop.dto;

import com.raota.domain.ramenShop.model.NormalMenu;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class NormalMenuDto {
    private Long id;
    private String name;
    private int price;
    private boolean signature;
    private String image_url;

    public static NormalMenuDto from(NormalMenu normalMenu){
        return from(normalMenu, normalMenu.getImageUrl());
    }

    public static NormalMenuDto from(NormalMenu normalMenu, String imageUrl){
        return new NormalMenuDto(
                normalMenu.getId(),
                normalMenu.getName(),
                normalMenu.getPrice(),
                normalMenu.getIsSignature(),
                imageUrl
        );
    }
}
