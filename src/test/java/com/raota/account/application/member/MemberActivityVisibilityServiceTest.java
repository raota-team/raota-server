package com.raota.account.application.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.raota.account.application.member.MemberActivityVisibilityService;
import com.raota.account.domain.member.model.MemberProfile;
import com.raota.account.domain.member.repository.MemberRepository;
import com.raota.global.cache.CacheInvalidationPublisher;
import com.raota.account.presentation.member.request.ActivityVisibilityUpdateRequest;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class MemberActivityVisibilityServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CacheInvalidationPublisher cacheInvalidationPublisher;

    @InjectMocks
    private MemberActivityVisibilityService service;

    @Test
    void updatesAndReturnsVisibilitySettings() {
        MemberProfile member = member(1L);
        given(memberRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(member));

        var result = service.update(
                1L,
                new ActivityVisibilityUpdateRequest(false, true, false, true)
        );

        assertThat(result.logs()).isFalse();
        assertThat(result.visits()).isTrue();
        assertThat(result.posts()).isFalse();
        assertThat(result.comments()).isTrue();
        verify(cacheInvalidationPublisher).publishAll("ramenShopList");
    }

    @Test
    void blocksOtherUserFromPrivateCategory() {
        MemberProfile member = member(1L);
        member.updateActivityVisibility(false, true, true, true);
        given(memberRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> service.requireLogsVisible(1L, 2L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("비공개 활동입니다.");
    }

    @Test
    void ownerCanReadPrivateCategory() {
        MemberProfile member = member(1L);
        member.updateActivityVisibility(false, false, false, false);

        service.requireLogsVisible(1L, 1L);
        service.requireVisitsVisible(1L, 1L);
        service.requirePostsVisible(1L, 1L);
        service.requireCommentsVisible(1L, 1L);
    }

    private MemberProfile member(Long id) {
        return MemberProfile.builder()
                .id(id)
                .nickname("공개설정테스터")
                .build();
    }
}
