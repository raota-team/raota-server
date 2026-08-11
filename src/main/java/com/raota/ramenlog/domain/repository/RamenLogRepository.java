package com.raota.ramenlog.domain.repository;

import com.raota.ramenlog.domain.model.RamenLog;
import com.raota.ramenlog.presentation.api.response.ProofPictureInfoResponse;
import com.raota.ramenlog.presentation.api.response.RamenShopProofPictureResponse;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RamenLogRepository extends JpaRepository<RamenLog, Long>, JpaSpecificationExecutor<RamenLog> {

    @EntityGraph(attributePaths = {"author", "ramenShop"})
    Optional<RamenLog> findByIdAndIsDeletedFalse(Long id);

    @EntityGraph(attributePaths = {"author", "ramenShop"})
    Page<RamenLog> findAll(org.springframework.data.jpa.domain.Specification<RamenLog> specification, Pageable pageable);

    long countByAuthorIdAndIsDeletedFalse(Long memberId);

    void deleteAllByAuthorId(Long memberId);

    long countByAuthorIdAndRamenShopIdAndIsDeletedFalse(Long memberId, Long shopId);
    long countByIsDeletedFalse();

    @Query(
            value = """
        select new com.raota.ramenlog.presentation.api.response.ProofPictureInfoResponse(
            p.id,
            true,
            p.imageUrl
        )
        from RamenLog p
        where p.author.id = :memberId and p.isDeleted = false
        order by p.visitedAt desc, p.createdAt desc
        """,
            countQuery = """
        select count(p) from RamenLog p
        where p.author.id = :memberId and p.isDeleted = false
        """
    )
    Page<ProofPictureInfoResponse> findMemberUploadPhoto(@Param("memberId") Long memberId, Pageable pageable);

    @Query(
            value = """
        select new com.raota.ramenlog.presentation.api.response.RamenShopProofPictureResponse(
            p.id,
            p.author.id,
            p.imageUrl,
            p.author.nickname,
            p.note,
            p.menuName,
            p.createdAt
        )
        from RamenLog p
        where p.ramenShop.id = :shopId
          and p.isDeleted = false
          and p.isPublic = true
          and p.author.activityVisibility.logsPublic = true
        order by p.visitedAt desc, p.createdAt desc
        """,
            countQuery = """
        select count(p) from RamenLog p
        where p.ramenShop.id = :shopId
          and p.isDeleted = false
          and p.isPublic = true
          and p.author.activityVisibility.logsPublic = true
        """
    )
    Page<RamenShopProofPictureResponse> searchPictures(@Param("shopId") Long shopId, Pageable pageable);

    @Query(value = """
        select
            ranked.ramen_shop_id as ramenShopId,
            ranked.image_url as imageUrl,
            ranked.ramen_log_count as ramenLogCount
        from (
            select
                p.ramen_shop_id,
                p.image_url,
                count(*) over (partition by p.ramen_shop_id) as ramen_log_count,
                row_number() over (
                    partition by p.ramen_shop_id
                    order by p.visited_at desc, p.created_at desc, p.id desc
                ) as preview_rank
            from tb_ramen_log p
            join tb_member_profile m on m.id = p.member_id
            where p.ramen_shop_id in (:shopIds)
              and p.is_deleted = false
              and p.is_public = true
              and m.logs_public = true
        ) ranked
        where ranked.preview_rank <= 3
        order by ranked.ramen_shop_id, ranked.preview_rank
        """, nativeQuery = true)
    List<RamenLogPreviewRow> findPreviewRowsByShopIds(@Param("shopIds") Collection<Long> shopIds);

    @Query("""
        select new com.raota.ramenlog.domain.repository.RecentVerifiedShopProjection(
            s.id,
            s.name,
            concat(s.address.city, concat(' ', s.address.district)),
            p.imageUrl,
            (select count(p2) from RamenLog p2
             where p2.ramenShop.id = s.id
               and p2.isDeleted = false
               and p2.isPublic = true
               and p2.author.activityVisibility.logsPublic = true)
        )
        from RamenLog p
        join p.ramenShop s
        where s.published = true
          and p.id in (
            select max(p3.id) from RamenLog p3
            where p3.isDeleted = false
              and p3.isPublic = true
              and p3.author.activityVisibility.logsPublic = true
            group by p3.ramenShop.id
        )
        order by p.visitedAt desc, p.createdAt desc
        """)
    List<RecentVerifiedShopProjection> findRecentVerifiedShops(Pageable pageable);

    interface RamenLogPreviewRow {
        Long getRamenShopId();
        String getImageUrl();
        long getRamenLogCount();
    }
}
