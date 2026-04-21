package com.raota.domain.member.service;

import com.raota.domain.member.controller.response.BookmarkSummaryResponse;
import com.raota.domain.member.controller.response.MyProfileResponse;
import com.raota.domain.member.controller.response.PhotoSummaryResponse;
import com.raota.domain.member.controller.response.VisitSummaryResponse;
import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.domain.ramenShop.repository.RamenProofPictureRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class MemberInfoService {

    private final MemberRepository memberRepository;
    private final RamenProofPictureRepository pictureRepository;

    public MyProfileResponse getMyProfile(Long memberId) {
        return memberRepository.findMemberDetailInfo(memberId);
    }

    @Transactional
    public MyProfileResponse updateMyProfile(
            String updateNickname,
            String updateImage,
            String updateBackgroundImage,
            String updateBio,
            Long memberId
    ) {
        MemberProfile member = memberRepository.findById(memberId).orElseThrow(()-> new IllegalArgumentException("없는 유저 정보 입니다."));

        member.updateProfile(updateNickname, updateImage, updateBackgroundImage, updateBio);
        memberRepository.save(member);

        return getMyProfile(memberId);
    }

    public Page<PhotoSummaryResponse> getMyPhotoList(Long memberId,Pageable pageable){
        return memberRepository.findMyPhotos(memberId,pageable);
    }

    public Page<BookmarkSummaryResponse> getMyBookmarks(Long memberId,Pageable pageable) {
        return memberRepository.findMyBookmarks(memberId,pageable);
    }

    public Page<VisitSummaryResponse> getMyVisits(Long memberId,Pageable pageable) {
        return memberRepository.findMyVisitRestaurant(memberId,pageable);
    }

    public Page<com.raota.domain.community.presentation.response.CommunityPostCardResponse> getMyPosts(Long memberId, Pageable pageable) {
        return memberRepository.findMyPosts(memberId, pageable);
    }

    public Page<com.raota.domain.community.presentation.response.CommunityCommentItemResponse> getMyComments(Long memberId, Pageable pageable) {
        return memberRepository.findMyComments(memberId, pageable);
    }
}
