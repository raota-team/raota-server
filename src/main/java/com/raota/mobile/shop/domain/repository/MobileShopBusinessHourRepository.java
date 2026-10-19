package com.raota.mobile.shop.domain.repository;

import com.raota.mobile.shop.domain.model.MobileShopBusinessHour;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MobileShopBusinessHourRepository extends JpaRepository<MobileShopBusinessHour, Long> {

    List<MobileShopBusinessHour> findByShopIdInOrderByDayOfWeekAsc(Collection<Long> shopIds);

    @Modifying(flushAutomatically = true)
    @Query("delete from MobileShopBusinessHour hour where hour.shopId = :shopId")
    void deleteByShopId(Long shopId);

}
