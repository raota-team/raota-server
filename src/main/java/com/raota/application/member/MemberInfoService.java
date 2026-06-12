package com.raota.application.member;

import com.raota.presentation.api.member.response.BookmarkSummaryResponse;
import com.raota.presentation.api.member.response.MemberSummaryResponse;
import com.raota.presentation.api.member.response.MyProfileResponse;
import com.raota.presentation.api.member.response.PhotoSummaryResponse;
import com.raota.presentation.api.member.response.VisitSummaryResponse;
import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.infrastructure.file.FileUploader;
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
    private final MemberProvisioningService memberProvisioningService;
    private final FileUploader fileUploader;

    public MyProfileResponse getMyProfile(Long memberId) {
        memberProvisioningService.getActiveRequired(memberId);
        MyProfileResponse response = memberRepository.findMemberDetailInfo(memberId);
        if (response == null) {
            throw new IllegalArgumentException("없는 유저 정보 입니다.");
        }
        return response;
    }

    public MemberSummaryResponse getMemberSummary(Long memberId) {
        MemberProfile member = memberProvisioningService.getActiveRequired(memberId);
        return new MemberSummaryResponse(
                member.getId(),
                member.getNickname(),
                fileUploader.getAccessibleUrl(member.getImageUrl())
        );
    }

    @Transactional
    public MyProfileResponse updateMyProfile(
            String updateNickname,
            String updateImage,
            String updateBackgroundImage,
            String updateBio,
            Long memberId
    ) {
        MemberProfile member = memberProvisioningService.getActiveRequired(memberId);

        member.updateProfile(updateNickname, updateImage, updateBackgroundImage, updateBio);
        memberRepository.save(member);

        return getMyProfile(memberId);
    }

    public Page<PhotoSummaryResponse> getMyPhotoList(Long memberId,Pageable pageable){
        return memberRepository.findMyPhotos(memberId,pageable)
                .map(photo -> new PhotoSummaryResponse(
                        photo.photo_id(),
                        fileUploader.getAccessibleUrl(photo.image_url()),
                        photo.menuName(),
                        photo.description(),
                        photo.restaurant_id(),
                        photo.restaurant_name(),
                        photo.uploaded_at()
                ));
    }

    public Page<BookmarkSummaryResponse> getMyBookmarks(Long memberId, Pageable pageable) {
        return memberRepository.findMyBookmarks(memberId, pageable)
                .map(bookmark -> new BookmarkSummaryResponse(
                        bookmark.restaurant_id(),
                        bookmark.restaurant_name(),
                        fileUploader.getAccessibleUrl(bookmark.restaurant_image_url()),
                        bookmark.address_simple(),
                        bookmark.bookmarked_at()
                ));
    }

    public Page<VisitSummaryResponse> getMyVisits(Long memberId,Pageable pageable) {
        return memberRepository.findMyVisitRestaurant(memberId,pageable)
                .map(visit -> new VisitSummaryResponse(
                        visit.restaurant_id(),
                        visit.restaurant_name(),
                        fileUploader.getAccessibleUrl(visit.restaurant_image_url()),
                        visit.simple_address(),
                        visit.visit_count_for_user(),
                        visit.last_visited_at()
                ));
    }

    public Page<com.raota.presentation.api.community.response.CommunityPostCardResponse> getMyPosts(Long memberId, Pageable pageable) {
        return memberRepository.findMyPosts(memberId, pageable);
    }

    public Page<com.raota.presentation.api.community.response.CommunityCommentItemResponse> getMyComments(Long memberId, Pageable pageable) {
        return memberRepository.findMyComments(memberId, pageable);
    }
}
