package com.raota.domain.ramenlog.repository;

import com.raota.domain.ramenlog.model.RamenLog;
import com.raota.presentation.api.ramenShop.response.ProofPictureInfoResponse;
import com.raota.presentation.api.ramenShop.response.RamenShopProofPictureResponse;
import com.raota.presentation.api.ramenShop.response.RecentVerifiedShopResponse;
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
        select new com.raota.presentation.api.ramenShop.response.ProofPictureInfoResponse(
            p.id,
            true,
            p.imageUrl
        )
        from RamenLog p
        where p.author.id = :memberId and p.isDeleted = false
        order by p.createdAt desc
        """,
            countQuery = """
        select count(p) from RamenLog p
        where p.author.id = :memberId and p.isDeleted = false
        """
    )
    Page<ProofPictureInfoResponse> findMemberUploadPhoto(@Param("memberId") Long memberId, Pageable pageable);

    @Query(
            value = """
        select new com.raota.presentation.api.ramenShop.response.RamenShopProofPictureResponse(
            p.id,
            p.author.id,
            p.imageUrl,
            p.author.nickname,
            p.note,
            p.menuName,
            p.createdAt
        )
        from RamenLog p
        where p.ramenShop.id = :shopId and p.isDeleted = false and p.isPublic = true
        order by p.createdAt desc
        """,
            countQuery = """
        select count(p) from RamenLog p
        where p.ramenShop.id = :shopId and p.isDeleted = false and p.isPublic = true
        """
    )
    Page<RamenShopProofPictureResponse> searchPictures(@Param("shopId") Long shopId, Pageable pageable);

    @Query("""
        select new com.raota.presentation.api.ramenShop.response.RecentVerifiedShopResponse(
            s.id,
            s.name,
            concat(s.address.city, concat(' ', s.address.district)),
            p.imageUrl,
            (select count(p2) from RamenLog p2
             where p2.ramenShop.id = s.id and p2.isDeleted = false and p2.isPublic = true)
        )
        from RamenLog p
        join p.ramenShop s
        where p.id in (
            select max(p3.id) from RamenLog p3
            where p3.isDeleted = false and p3.isPublic = true
            group by p3.ramenShop.id
        )
        order by p.createdAt desc
        """)
    List<RecentVerifiedShopResponse> findRecentVerifiedShops(Pageable pageable);
}
