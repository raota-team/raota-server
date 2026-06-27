package com.raota.domain.ramenShop.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tb_ramen_shop")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Getter
public class RamenShop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ramen_shop_id")
    private Long id;

    @Column(name = "naver_map_id")
    private String naverMapId;

    @Column(nullable = false)
    private String name;

    @Column(name = "branch_name")
    private String branchName;

    @Embedded
    private Address address;

    @Embedded
    private BusinessHours businessHours;

    @Embedded
    @Builder.Default
    private ShopStats stats = ShopStats.init();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "json")
    private List<String> tags;

    @Column(name = "instagram_url")
    private String instagramUrl;

    @Column(name = "catch_table_url")
    private String catchTableUrl;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "detailed_description", columnDefinition = "text")
    private String detailedDescription;

    @Column(name = "image_url")
    private String imageUrl;

    @Builder.Default
    @Column(name = "is_published", nullable = false)
    private boolean published = true;

    @Embedded
    @Builder.Default
    private NormalMenus normalMenus = NormalMenus.init();

    @Embedded
    @Builder.Default
    private EventMenus eventMenus = EventMenus.init();

    public void addNormalMenu(NormalMenu menu) {
        if (normalMenus == null) normalMenus = NormalMenus.init();
        normalMenus.add(menu);
        menu.setShop(this);
    }

    public void addEventMenu(EventMenu eventMenu){
        if (eventMenus == null) eventMenus = EventMenus.init();
        eventMenus.add(eventMenu);
        eventMenu.setShop(this);
    }

    public void updateBasicInfo(String name, String branchName, String naverMapId, Address address, BusinessHours businessHours,
                                List<String> tags, String instagramUrl, String catchTableUrl,
                                String description, String imageUrl) {
        updateBasicInfo(
                name,
                branchName,
                naverMapId,
                address,
                businessHours,
                tags,
                instagramUrl,
                catchTableUrl,
                description,
                this.detailedDescription,
                imageUrl
        );
    }

    public void updateBasicInfo(String name, String branchName, String naverMapId, Address address, BusinessHours businessHours,
                                List<String> tags, String instagramUrl, String catchTableUrl,
                                String description, String detailedDescription, String imageUrl) {
        this.name = Objects.requireNonNull(name, "name");
        this.branchName = branchName;
        this.naverMapId = naverMapId;
        this.address = Objects.requireNonNull(address, "address");
        this.businessHours = businessHours;
        this.tags = tags;
        this.instagramUrl = instagramUrl;
        this.catchTableUrl = catchTableUrl;
        this.description = description;
        this.detailedDescription = detailedDescription;
        this.imageUrl = imageUrl;
    }

    public void replaceNormalMenus(List<NormalMenu> menus) {
        if (normalMenus == null) {
            normalMenus = NormalMenus.init();
        }
        normalMenus.clear();
        menus.forEach(this::addNormalMenu);
    }

    public void replaceEventMenus(List<EventMenu> menus) {
        if (eventMenus == null) {
            eventMenus = EventMenus.init();
        }
        eventMenus.clear();
        menus.forEach(this::addEventMenu);
    }

    public void updatePublished(boolean published) {
        this.published = published;
    }

    public void increaseBookmarkCount() {
        if (this.stats == null) this.stats = ShopStats.init();
        this.stats = this.stats.increaseBookmark();
    }

    public void decreaseBookmarkCount() {
        if (this.stats == null) this.stats = ShopStats.init();
        this.stats = this.stats.decreaseBookmark();
    }

    public void increaseViewCount() {
        if (this.stats == null) this.stats = ShopStats.init();
        this.stats = this.stats.increaseView();
    }

    public void increaseVisitCount() {
        if (this.stats == null) this.stats = ShopStats.init();
        this.stats = this.stats.increaseVisit();
    }

    public void decreaseVisitCount() {
        if (this.stats == null) this.stats = ShopStats.init();
        this.stats = this.stats.decreaseVisit();
    }
}
