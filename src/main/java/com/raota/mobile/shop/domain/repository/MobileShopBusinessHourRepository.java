package com.raota.mobile.shop.domain.repository;

import com.raota.mobile.shop.domain.model.MobileShopBusinessHour;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MobileShopBusinessHourRepository extends JpaRepository<MobileShopBusinessHour, Long> {

    List<MobileShopBusinessHour> findByShopIdInOrderByDayOfWeekAsc(Collection<Long> shopIds);

}
