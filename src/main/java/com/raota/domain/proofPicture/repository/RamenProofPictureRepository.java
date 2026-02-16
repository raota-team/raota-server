package com.raota.domain.proofPicture.repository;

import com.raota.domain.proofPicture.controller.response.ProofPictureInfoResponse;
import com.raota.domain.proofPicture.model.RamenProofPicture;
import com.raota.domain.proofPicture.controller.response.RamenShopProofPictureResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RamenProofPictureRepository extends JpaRepository<RamenProofPicture,Long>{
    @Query(
            value = """
        select new com.raota.domain.proofPicture.controller.response.ProofPictureInfoResponse(
            p.id,
            true,
            p.imageUrl
        )
        from RamenProofPicture p
        where p.memberProfile.id = :memberId
        order by p.uploadAt desc
        """,
            countQuery = """
        select count(p)
        from RamenProofPicture p
        where p.memberProfile.id = :memberId
        """
    )
    Page<ProofPictureInfoResponse> findMemberUploadPhoto(@Param("memberId") Long memberId, Pageable pageable);

    @Query(
            value = """
        select new com.raota.domain.proofPicture.controller.response.RamenShopProofPictureResponse(
            p.id,
            p.memberProfile.id,
            p.imageUrl,
            p.memberProfile.nickname,
            p.imageUrl,
            p.imageName,
            p.uploadAt
        )
        from RamenProofPicture p
        where p.ramenShop.id = :shopId
        order by p.uploadAt desc
        """,
            countQuery = """
        select count(p)
        from RamenProofPicture p
        where p.ramenShop.id = :shopId
        """
    )
    Page<RamenShopProofPictureResponse> searchPictures(@Param("shopId") Long shopId, Pageable pageable);
}
