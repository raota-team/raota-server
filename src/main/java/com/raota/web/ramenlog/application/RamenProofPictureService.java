package com.raota.web.ramenlog.application;

import com.raota.global.cache.CacheInvalidationPublisher;
import com.raota.global.file.FileUploader;
import com.raota.web.account.domain.member.model.MemberProfile;
import com.raota.web.account.domain.member.repository.MemberRepository;
import com.raota.web.ramenlog.domain.model.RamenLog;
import com.raota.web.ramenlog.domain.repository.RamenLogRepository;
import com.raota.web.ramenlog.presentation.api.response.RamenShopProofPictureResponse;
import com.raota.web.ramenshop.domain.model.RamenShop;
import com.raota.web.ramenshop.domain.repository.RamenShopRepository;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class RamenProofPictureService {

    private final RamenLogRepository ramenLogRepository;

    private final MemberRepository memberRepository;

    private final RamenShopRepository ramenShopRepository;

    private final FileUploader fileUploader;

    private final CacheInvalidationPublisher cacheInvalidationPublisher;

    public Page<RamenShopProofPictureResponse> findProofPicture(Long shopId, Pageable pageable) {
        return ramenLogRepository.searchPictures(shopId, pageable);
    }

    @Transactional
    public void deletePicture(Long photoId, Long memberId) {
        RamenLog proofPicture = ramenLogRepository
                .findByIdAndIsDeletedFalse(photoId)
                .orElseThrow(() -> new IllegalArgumentException("찾을수 없는 사진입니다."));

        if (!Objects.equals(memberId, proofPicture.getAuthor().getId())) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }

        MemberProfile member =
                memberRepository.findById(memberId).orElseThrow(() -> new IllegalArgumentException("없는 유저입니다."));
        RamenShop ramenShop = ramenShopRepository
                .findById(proofPicture.getRamenShop().getId())
                .orElseThrow(() -> new IllegalArgumentException("없는 라멘집 입니다."));

        // 삭제 전, 이 가게에 남은 사진이 1개뿐이라면 '방문한 식당' 수 감소
        long currentPhotosInShop =
                ramenLogRepository.countByAuthorIdAndRamenShopIdAndIsDeletedFalse(memberId, ramenShop.getId());
        if (currentPhotosInShop == 1) {
            member.decreaseVisitedRestaurantCount();
        }

        member.decreasePhotoCount();
        ramenShop.decreaseVisitCount();
        proofPicture.delete();
        cacheInvalidationPublisher.publish("ramenShopDetail", String.valueOf(ramenShop.getId()));
        cacheInvalidationPublisher.publishAll("ramenShopList");
    }
}
