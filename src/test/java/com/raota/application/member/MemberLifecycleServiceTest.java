package com.raota.application.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.raota.application.auth.AuthAccountService;
import com.raota.domain.auth.repository.SocialAccountRepository;
import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.BookmarkRepository;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.domain.ramenlog.repository.RamenLogLikeRepository;
import com.raota.domain.ramenlog.repository.RamenLogRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberLifecycleServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private MemberProvisioningService memberProvisioningService;
    @Mock
    private AuthAccountService authAccountService;
    @Mock
    private SocialAccountRepository socialAccountRepository;
    @Mock
    private BookmarkRepository bookmarkRepository;
    @Mock
    private RamenLogLikeRepository ramenLogLikeRepository;
    @Mock
    private RamenLogRepository ramenLogRepository;

    @InjectMocks
    private MemberLifecycleService memberLifecycleService;

    @Test
    @DisplayName("회원 탈퇴 시 deleted_at을 기록하고 리프레시 토큰을 제거한다")
    void withdraw() {
        MemberProfile member = MemberProfile.builder()
                .id(1L)
                .nickname("tester")
                .build();
        given(memberProvisioningService.getActiveRequired(1L)).willReturn(member);

        memberLifecycleService.withdraw(1L);

        assertThat(member.getDeletedAt()).isNotNull();
        verify(authAccountService).logoutByMemberId(1L);
    }

    @Test
    @DisplayName("30일이 지난 탈퇴 회원은 커뮤니티 데이터는 남기고 프로필을 익명화한다")
    void purgeExpiredMembers() {
        MemberProfile expiredMember = MemberProfile.builder()
                .id(10L)
                .nickname("expired")
                .build();
        expiredMember.softDelete(LocalDateTime.now().minusDays(91));

        given(memberRepository.findSoftDeletedMembersDueForPurge(any()))
                .willReturn(List.of(expiredMember));

        int purgedCount = memberLifecycleService.purgeExpiredMembers();

        assertThat(purgedCount).isEqualTo(1);
        verify(authAccountService).logoutByMemberId(10L);
        verify(socialAccountRepository).deleteByMemberId(10L);
        verify(bookmarkRepository).deleteAllByMemberProfileId(10L);
        verify(ramenLogLikeRepository).deleteAllByMemberId(10L);
        verify(ramenLogLikeRepository).deleteAllByRamenLogAuthorId(10L);
        verify(ramenLogRepository).deleteAllByAuthorId(10L);
        verify(memberRepository, never()).delete(expiredMember);
        assertThat(expiredMember.getNickname()).isEqualTo("탈퇴한 사용자");
        assertThat(expiredMember.getImageUrl()).isNull();
        assertThat(expiredMember.getBackgroundImageUrl()).isNull();
        assertThat(expiredMember.getBio()).isNull();
    }

    @Test
    @DisplayName("정리 대상이 없으면 아무 것도 삭제하지 않는다")
    void purgeExpiredMembers_noTarget() {
        given(memberRepository.findSoftDeletedMembersDueForPurge(any()))
                .willReturn(List.of());

        int purgedCount = memberLifecycleService.purgeExpiredMembers();

        assertThat(purgedCount).isZero();
        verify(authAccountService, never()).logoutByMemberId(any());
        verify(memberRepository, never()).delete(any(MemberProfile.class));
    }
}
