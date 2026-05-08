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
            s.stats.visitCount
        )
        from RamenShop s
        where (:city is null or :city = '' or s.address.city = :city)
          and (:district is null or :district = '' or s.address.district = :district)
          and (:keyword is null or :keyword = '' or s.name like concat('%', :keyword, '%'))
          and (:tag is null or :tag = '' or function('json_search', s.tags, 'all', concat('%', :tag, '%')) is not null)
        """,
            countQuery = """
        select count(s)
        from RamenShop s
        where (:city is null or :city = '' or s.address.city = :city)
          and (:district is null or :district = '' or s.address.district = :district)
          and (:keyword is null or :keyword = '' or s.name like concat('%', :keyword, '%'))
          and (:tag is null or :tag = '' or function('json_search', s.tags, 'all', concat('%', :tag, '%')) is not null)
        """
    )
    Page<StoreSummaryResponse> searchStores(@Param("city") String city,
                                            @Param("district") String district,
                                            @Param("keyword") String keyword,
                                            @Param("tag") String tag, Pageable pageable);
}
