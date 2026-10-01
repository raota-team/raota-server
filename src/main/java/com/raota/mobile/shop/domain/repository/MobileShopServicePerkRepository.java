package com.raota.mobile.shop.domain.repository;

import com.raota.mobile.shop.domain.model.MobileShopServicePerk;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MobileShopServicePerkRepository extends JpaRepository<MobileShopServicePerk, Long> {

    List<MobileShopServicePerk> findByShopIdOrderByIdAsc(Long shopId);

}
