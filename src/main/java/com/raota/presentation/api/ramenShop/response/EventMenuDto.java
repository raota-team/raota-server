package com.raota.presentation.api.ramenShop.response;

import com.raota.domain.ramenShop.model.EventMenu;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class EventMenuDto {
    private Long id;
    private String name;
    private String description;
    private int price;
    private String badge_text;

    public static EventMenuDto from(EventMenu eventMenu){
        return new EventMenuDto(
                eventMenu.getId(),
                eventMenu.getName(),
                eventMenu.getDescription(),
                eventMenu.getPrice(),
                eventMenu.getBadgeText()
        );
    }
}
