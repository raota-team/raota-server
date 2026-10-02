package com.raota.mobile.shop.domain.repository;

import com.raota.mobile.shop.domain.model.MobileShopImage;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MobileShopImageRepository extends JpaRepository<MobileShopImage, Long> {

    List<MobileShopImage> findByShopIdInOrderBySortOrderAscIdAsc(Collection<Long> shopIds);

}
