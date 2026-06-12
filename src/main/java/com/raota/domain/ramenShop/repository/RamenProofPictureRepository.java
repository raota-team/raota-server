package com.raota.domain.ramenShop.repository;

import com.raota.presentation.api.ramenShop.response.ProofPictureInfoResponse;
import com.raota.presentation.api.ramenShop.response.RamenShopProofPictureResponse;
import com.raota.domain.ramenShop.model.RamenProofPicture;
import com.raota.presentation.api.ramenShop.response.RecentVerifiedShopResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RamenProofPictureRepository extends JpaRepository<RamenProofPicture, Long> {
    void deleteAllByMemberProfileId(Long memberId);

    @Query(
            value = """
        select new com.raota.presentation.api.ramenShop.response.ProofPictureInfoResponse(
            p.id,
            true,
            p.imageUrl
        )
        from RamenProofPicture p
        where p.memberProfile.id = :memberId
                and p.isDeleted = false
        order by p.uploadedAt desc
        """,
            countQuery = """
        select count(p)
        from RamenProofPicture p
        where p.memberProfile.id = :memberId
                and p.isDeleted = false
        """
    )
    Page<ProofPictureInfoResponse> findMemberUploadPhoto(@Param("memberId") Long memberId, Pageable pageable);

    @Query(
            value = """
        select new com.raota.presentation.api.ramenShop.response.RamenShopProofPictureResponse(
            p.id,
            p.memberProfile.id,
            p.imageUrl,
            p.memberProfile.nickname,
            p.description,
            p.menuName,
            p.uploadedAt
        )
        from RamenProofPicture p
        where p.ramenShop.id = :shopId
                and p.isDeleted = false
        order by p.uploadedAt desc
        """,
            countQuery = """
        select count(p)
        from RamenProofPicture p
        where p.ramenShop.id = :shopId
                and p.isDeleted = false
        """
    )
    Page<RamenShopProofPictureResponse> searchPictures(@Param("shopId") Long shopId, Pageable pageable);

    @Query(value = """
        select new com.raota.presentation.api.ramenShop.response.RecentVerifiedShopResponse(
            s.id,
            s.name,
            concat(s.address.city, concat(' ', s.address.district)),
            p.imageUrl,
            (select count(p2) from RamenProofPicture p2 where p2.ramenShop.id = s.id and p2.isDeleted = false)
        )
        from RamenProofPicture p
        join p.ramenShop s
        where p.id in (
            select max(p3.id)
            from RamenProofPicture p3
            where p3.isDeleted = false
            group by p3.ramenShop.id
        )
        order by p.uploadedAt desc
        """)
    List<RecentVerifiedShopResponse> findRecentVerifiedShops(Pageable pageable);

    long countByMemberProfileIdAndRamenShopIdAndIsDeletedFalse(Long memberId, Long shopId);
}
