package com.raota.domain.ramenShop.repository;

import com.raota.domain.ramenShop.controller.response.ProofPictureInfoResponse;
import com.raota.domain.ramenShop.controller.response.RamenShopProofPictureResponse;
import com.raota.domain.ramenShop.model.RamenProofPicture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RamenProofPictureRepository extends JpaRepository<RamenProofPicture, Long> {
    @Query(
            value = """
        select new com.raota.domain.ramenShop.controller.response.ProofPictureInfoResponse(
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
        select new com.raota.domain.ramenShop.controller.response.RamenShopProofPictureResponse(
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

    long countByMemberProfileIdAndRamenShopIdAndIsDeletedFalse(Long memberId, Long shopId);
}
