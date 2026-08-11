package com.raota.ramenshop.domain.model;


import com.raota.ramenshop.presentation.response.NormalMenuDto;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Embeddable
@AllArgsConstructor
@NoArgsConstructor
public class NormalMenus {

    @OneToMany(mappedBy = "ramenShop", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NormalMenu> values;

    public static NormalMenus init() {
        return new NormalMenus(new ArrayList<>());
    }

    public List<NormalMenuDto> getNormalMenusInfo(){
        return menus().stream().map(NormalMenuDto::from).toList();
    }

    public void add(NormalMenu normalMenu){
        verifyMenuNameDuplicate(normalMenu.getName());
        menus().add(normalMenu);
    }

    public Optional<NormalMenu> findMenuById(Long menuId){
        return menus().stream()
                .filter(menu -> menu.getId().equals(menuId))
                .findFirst();
    }

    public List<NormalMenu> getValues() {
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

    private List<NormalMenu> menus() {
        if (values == null) {
            values = new ArrayList<>();
        }
        return values;
    }
}
