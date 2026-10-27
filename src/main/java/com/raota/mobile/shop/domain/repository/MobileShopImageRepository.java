package com.raota.mobile.shop.domain.repository;

import com.raota.mobile.shop.domain.model.MobileShopImage;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MobileShopImageRepository extends JpaRepository<MobileShopImage, Long> {

    List<MobileShopImage> findByShopIdInOrderBySortOrderAscIdAsc(Collection<Long> shopIds);

    @Modifying(flushAutomatically = true)
    @Query("delete from MobileShopImage image where image.shopId = :shopId")
    void deleteByShopId(Long shopId);
}
