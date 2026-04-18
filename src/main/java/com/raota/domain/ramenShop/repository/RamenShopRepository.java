package com.raota.domain.ramenShop.repository;

import com.raota.domain.ramenShop.controller.response.StoreSummaryResponse;
import com.raota.domain.ramenShop.model.RamenShop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RamenShopRepository extends JpaRepository<RamenShop, Long> {

    @Query(
            value = """
        select new com.raota.domain.ramenShop.controller.response.StoreSummaryResponse(
            s.id,
            s.name,
            s.description,
            concat(s.address.city, concat(' ', s.address.district)),
            s.tags,
            s.imageUrl,
            s.stats.bookmarkCount
        )
        from RamenShop s
        where (:region is null or :region = '' or s.address.city = :region)
          and (:keyword is null or :keyword = '' or s.name like concat('%', :keyword, '%'))
        """,
            countQuery = """
        select count(s)
        from RamenShop s
        where (:region is null or :region = '' or s.address.city = :region)
          and (:keyword is null or :keyword = '' or s.name like concat('%', :keyword, '%'))
        """
    )
    Page<StoreSummaryResponse> searchStores(@Param("region") String region,
                                            @Param("keyword") String keyword,
                                            Pageable pageable);
}
