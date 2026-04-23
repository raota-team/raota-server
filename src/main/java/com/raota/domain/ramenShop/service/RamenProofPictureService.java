package com.raota.domain.ramenShop.service;

import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.domain.ramenShop.controller.response.ProofPictureInfoResponse;
import com.raota.domain.ramenShop.model.RamenProofPicture;
import com.raota.domain.ramenShop.repository.RamenProofPictureRepository;
import com.raota.domain.ramenShop.controller.response.RamenShopProofPictureResponse;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import com.raota.global.file.FileUploader;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@AllArgsConstructor
public class RamenProofPictureService {

    private final RamenProofPictureRepository proofPictureRepository;
    private final MemberRepository memberRepository;
    private final RamenShopRepository ramenShopRepository;
    private final FileUploader fileUploader;

    @Transactional
    public ProofPictureInfoResponse addProofPicture(Long shopId, String imageUrl, String description, Long memberId) {
        MemberProfile member = memberRepository.findById(memberId).orElseThrow(()->new IllegalArgumentException("없는 유저 입니다."));
        RamenShop ramenShop = ramenShopRepository.findById(shopId).orElseThrow(()->new IllegalArgumentException("없는 라멘집 입니다."));

        RamenProofPicture picture = RamenProofPicture.builder()
                .ramenShop(ramenShop)
                .memberProfile(member)
                .imageUrl(imageUrl)
                .description(description)
                .build();

        RamenProofPicture saved = proofPictureRepository.save(picture);

        member.increasePhotoCount();

        return new ProofPictureInfoResponse(
                saved.getId(),
                true,
                saved.getImageUrl()
        );
    }

    public Page<RamenShopProofPictureResponse> findProofPicture(Long shopId, Pageable pageable) {
        return proofPictureRepository.searchPictures(shopId,pageable);
    }
}
