package com.raota.domain.proofPicture.service;

import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.domain.proofPicture.controller.response.ProofPictureInfoResponse;
import com.raota.domain.proofPicture.model.RamenProofPicture;
import com.raota.domain.proofPicture.repository.RamenProofPictureRepository;
import com.raota.domain.proofPicture.controller.response.RamenShopProofPictureResponse;
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
    public ProofPictureInfoResponse addProofPicture(Long shopId, MultipartFile file,Long memberId) {
        MemberProfile member = memberRepository.findById(memberId).orElseThrow(()->new IllegalArgumentException("없는 유저 입니다."));
        RamenShop ramenShop = ramenShopRepository.findById(shopId).orElseThrow(()->new IllegalArgumentException("없는 라멘집 입니다."));

        String imageUrl = fileUploader.upload(file,"ramen");

        RamenProofPicture picture = RamenProofPicture.builder()
                .ramenShop(ramenShop)
                .memberProfile(member)
                .imageName(file.getOriginalFilename())
                .imageUrl(imageUrl)
                .build();

        RamenProofPicture saved = proofPictureRepository.save(picture);

        return new ProofPictureInfoResponse(
                null,
                null,
                null
        );
    }

    public Page<RamenShopProofPictureResponse> findProofPicture(Long shopId, Pageable pageable) {
        return proofPictureRepository.searchPictures(shopId,pageable);
    }
}
