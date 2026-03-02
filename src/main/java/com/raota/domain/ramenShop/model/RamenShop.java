package com.raota.domain.ramenShop.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Getter
public class RamenShop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ramen_shop_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Embedded
    private Address address;

    @Embedded
    private BusinessHours businessHours;

    @Embedded
    @Builder.Default
    private ShopStats stats = ShopStats.init();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags",columnDefinition = "json")
    private List<String> tags;

    @Column(name = "instagram_url")
    private String instagramUrl;

    @Column(name = "image_url")
    private String imageUrl;

    @Embedded
    @Builder.Default
    private NormalMenus normalMenus = NormalMenus.init();

    @Embedded
    @Builder.Default
    private EventMenus eventMenus = EventMenus.init();

    public void addNormalMenu(NormalMenu menu) {
        normalMenus.add(menu);
        menu.setShop(this);
    }

    public void addEventMenu(EventMenu eventMenu){
        eventMenus.add(eventMenu);
        eventMenu.setShop(this);
    }
}
