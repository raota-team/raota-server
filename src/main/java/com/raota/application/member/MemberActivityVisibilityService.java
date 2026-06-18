package com.raota.application.member;

import com.raota.domain.member.model.MemberActivityVisibility;
import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.presentation.api.member.request.ActivityVisibilityUpdateRequest;
import com.raota.presentation.api.member.response.ActivityVisibilityResponse;
import jakarta.persistence.EntityNotFoundException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberActivityVisibilityService {

    private final MemberRepository memberRepository;

    public ActivityVisibilityResponse get(Long memberId) {
        return ActivityVisibilityResponse.from(requireMember(memberId).getActivityVisibility());
    }

    @Transactional
    public ActivityVisibilityResponse update(Long memberId, ActivityVisibilityUpdateRequest request) {
        MemberProfile member = requireMember(memberId);
        member.updateActivityVisibility(
                request.logs(),
                request.visits(),
                request.posts(),
                request.comments()
        );
        return ActivityVisibilityResponse.from(member.getActivityVisibility());
    }

    public void requireLogsVisible(Long targetMemberId, Long viewerId) {
        requireVisible(targetMemberId, viewerId, Category.LOGS);
    }

    public void requireVisitsVisible(Long targetMemberId, Long viewerId) {
        requireVisible(targetMemberId, viewerId, Category.VISITS);
    }

    public void requirePostsVisible(Long targetMemberId, Long viewerId) {
        requireVisible(targetMemberId, viewerId, Category.POSTS);
    }

    public void requireCommentsVisible(Long targetMemberId, Long viewerId) {
        requireVisible(targetMemberId, viewerId, Category.COMMENTS);
    }

    private void requireVisible(Long targetMemberId, Long viewerId, Category category) {
        if (Objects.equals(targetMemberId, viewerId)) {
            return;
        }

        MemberActivityVisibility visibility = requireMember(targetMemberId).getActivityVisibility();
        boolean visible = visibility == null || switch (category) {
            case LOGS -> visibility.isLogsPublic();
            case VISITS -> visibility.isVisitsPublic();
            case POSTS -> visibility.isPostsPublic();
            case COMMENTS -> visibility.isCommentsPublic();
        };
        if (!visible) {
            throw new AccessDeniedException("비공개 활동입니다.");
        }
    }

    private MemberProfile requireMember(Long memberId) {
        return memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
    }

    private enum Category {
        LOGS,
        VISITS,
        POSTS,
        COMMENTS
    }
}
