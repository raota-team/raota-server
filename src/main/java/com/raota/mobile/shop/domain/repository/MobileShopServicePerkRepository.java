package com.raota.mobile.shop.domain.repository;

import com.raota.mobile.shop.domain.model.MobileShopServicePerk;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MobileShopServicePerkRepository extends JpaRepository<MobileShopServicePerk, Long> {

    List<MobileShopServicePerk> findByShopIdOrderByIdAsc(Long shopId);

    @Modifying(flushAutomatically = true)
    @Query("delete from MobileShopServicePerk perk where perk.shopId = :shopId")
    void deleteByShopId(Long shopId);

}
