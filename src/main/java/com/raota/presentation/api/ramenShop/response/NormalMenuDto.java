package com.raota.presentation.api.ramenShop.response;

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

    public static NormalMenuDto from(NormalMenu normalMenu){
        return new NormalMenuDto(
                normalMenu.getId(),
                normalMenu.getName(),
                normalMenu.getPrice(),
                normalMenu.getIsSignature()
        );
    }
}
