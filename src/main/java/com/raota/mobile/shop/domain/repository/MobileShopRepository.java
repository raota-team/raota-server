package com.raota.mobile.shop.domain.repository;

import com.raota.mobile.shop.domain.model.MobileShop;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** 공개 상태의 매장만 외부 조회에 내보낸다. */
public interface MobileShopRepository extends JpaRepository<MobileShop, Long> {

    Optional<MobileShop> findByIdAndPublishedTrueAndDeletedAtIsNull(Long id);

    List<MobileShop> findByPublishedTrueAndDeletedAtIsNull();

    /** 엔티티의 오래된 조회수를 저장하지 않고 DB에서 원자적으로 증가시킨다. */
    @Modifying
    @Query("update MobileShop shop set shop.viewCount = shop.viewCount + 1 where shop.id = :id")
    void incrementViewCount(Long id);

}
