package com.raota.web.ramenshop.presentation.response;

import com.raota.web.ramenshop.domain.model.NormalMenu;
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
