package com.raota.ramenshop.domain.model;

import com.raota.ramenshop.presentation.response.EventMenuDto;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Embeddable
@AllArgsConstructor
@NoArgsConstructor
public class EventMenus {

    @OneToMany(mappedBy = "ramenShop", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventMenu> values;

    public static EventMenus init() {
        return new EventMenus(new ArrayList<>());
    }

    public List<EventMenuDto> getEventMenusInfo(){
        return menus().stream().map(EventMenuDto::from).toList();
    }

    public void add(EventMenu eventMenu){
        verifyMenuNameDuplicate(eventMenu.getName());
        menus().add(eventMenu);
    }

    public List<EventMenu> getValues() {
        return List.copyOf(menus());
    }

    public void clear() {
        menus().clear();
    }

    private void verifyMenuNameDuplicate(String name){
        if (menus().stream().anyMatch(menu -> menu.getName().equals(name))) {
            throw new IllegalArgumentException("이미 존재하는 메뉴 이름입니다: " + name);
        }
    }

    private List<EventMenu> menus() {
        if (values == null) {
            values = new ArrayList<>();
        }
        return values;
    }
}
