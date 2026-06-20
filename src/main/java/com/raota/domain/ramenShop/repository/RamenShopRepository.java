package com.raota.domain.ramenShop.repository;

import com.raota.presentation.api.ramenShop.response.RamenShopResponse;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.presentation.api.discovery.response.TodayPopularRamenShopResponse;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RamenShopRepository extends JpaRepository<RamenShop, Long> {

    Optional<RamenShop> findByIdAndPublishedTrue(Long id);

    List<RamenShop> findAllByPublishedTrue();

    List<RamenShop> findAllByIdBetweenOrderByIdAsc(Long fromId, Long toId);

    long countByPublishedTrue();

    @Query(
            value = """
        select new com.raota.presentation.api.ramenShop.response.RamenShopResponse(
            s.id,
            s.name,
            s.description,
            concat(s.address.city, concat(' ', s.address.district)),
            s.tags,
            s.imageUrl,
            s.stats.visitCount,
            s.stats.viewCount
        )
        from RamenShop s
        where s.published = true
          and (:city is null or :city = '' or s.address.city = :city)
          and (:district is null or :district = '' or s.address.district = :district)
          and (:keyword is null or :keyword = '' or s.name like concat('%', :keyword, '%'))
          and (
              :tag is null or :tag = '' or exists (
                  select 1
                  from NormalMenu m
                  where m.ramenShop = s
                    and m.name like concat('%', :tag, '%')
              )
          )
        """,
            countQuery = """
        select count(s)
        from RamenShop s
        where s.published = true
          and (:city is null or :city = '' or s.address.city = :city)
          and (:district is null or :district = '' or s.address.district = :district)
          and (:keyword is null or :keyword = '' or s.name like concat('%', :keyword, '%'))
          and (
              :tag is null or :tag = '' or exists (
                  select 1
                  from NormalMenu m
                  where m.ramenShop = s
                    and m.name like concat('%', :tag, '%')
              )
          )
        """
    )
    Page<RamenShopResponse> searchStores(@Param("city") String city,
                                         @Param("district") String district,
                                         @Param("keyword") String keyword,
                                         @Param("tag") String tag, Pageable pageable);

    @Query("""
            select new com.raota.presentation.api.discovery.response.TodayPopularRamenShopResponse(
                s.id,
                s.name
            )
            from RamenShop s
            where s.id in :shopIds
              and s.published = true
            """)
    List<TodayPopularRamenShopResponse> findPopularTodayShops(@Param("shopIds") Collection<Long> shopIds);
}
