package com.raota.mobile.shop.domain.repository;

import com.raota.mobile.shop.domain.model.MobileShop;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 공개 상태의 매장만 외부 조회에 내보낸다. */
public interface MobileShopRepository extends JpaRepository<MobileShop, Long> {

    Optional<MobileShop> findByIdAndPublishedTrueAndDeletedAtIsNull(Long id);

}
