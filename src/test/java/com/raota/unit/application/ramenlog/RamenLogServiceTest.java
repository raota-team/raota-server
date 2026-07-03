package com.raota.unit.application.ramenlog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.domain.ramenShop.model.Address;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import com.raota.domain.ramenlog.model.RamenLog;
import com.raota.domain.ramenlog.model.RamenLogLike;
import com.raota.domain.ramenlog.model.RevisitIntention;
import com.raota.domain.ramenlog.repository.RamenLogLikeRepository;
import com.raota.domain.ramenlog.repository.RamenLogRepository;
import com.raota.infrastructure.file.FileUploader;
import com.raota.infrastructure.cache.CacheInvalidationPublisher;
import com.raota.application.member.MemberActivityVisibilityService;
import com.raota.application.ramenlog.RamenLogService;
import com.raota.presentation.api.ramenlog.request.RamenLogUpsertRequest;
import com.raota.presentation.api.ramenlog.response.RamenLogLikeResponse;
import com.raota.presentation.api.ramenlog.response.RamenLogResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class RamenLogServiceTest {

    @Mock private RamenLogRepository ramenLogRepository;
    @Mock private RamenLogLikeRepository ramenLogLikeRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private RamenShopRepository ramenShopRepository;
    @Mock private FileUploader fileUploader;
    @Mock private CacheInvalidationPublisher cacheInvalidationPublisher;
    @Mock private MemberActivityVisibilityService memberActivityVisibilityService;
    @InjectMocks private RamenLogService ramenLogService;

    @Test
    void createsRamenLogFromFrontendPayload() {
        MemberProfile member = member(1L);
        RamenShop shop = shop(10L);
        given(memberRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(member));
        given(ramenShopRepository.findById(10L)).willReturn(Optional.of(shop));
        given(ramenLogRepository.countByAuthorIdAndRamenShopIdAndIsDeletedFalse(1L, 10L)).willReturn(0L);
        given(ramenLogRepository.save(any(RamenLog.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(fileUploader.getAccessibleUrl(any())).willAnswer(invocation -> invocation.getArgument(0));

        RamenLogResponse response = ramenLogService.create(request(true), 1L);

        ArgumentCaptor<RamenLog> captor = ArgumentCaptor.forClass(RamenLog.class);
        verify(ramenLogRepository).save(captor.capture());
        RamenLog saved = captor.getValue();
        assertThat(saved.getMenuName()).isEqualTo("특제 돈코츠");
        assertThat(saved.getVisitedAt()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(saved.getBrothNotes()).containsExactly("진해요");
        assertThat(response.mine()).isTrue();
        assertThat(response.isPublic()).isTrue();
    }

    @Test
    void togglesLikeOnAndOff() {
        MemberProfile author = member(1L);
        MemberProfile liker = member(2L);
        RamenLog log = log(100L, author, true);
        given(ramenLogRepository.findByIdAndIsDeletedFalse(100L)).willReturn(Optional.of(log));
        given(memberRepository.findByIdAndDeletedAtIsNull(2L)).willReturn(Optional.of(liker));
        given(ramenLogLikeRepository.findByRamenLogIdAndMemberId(100L, 2L))
                .willReturn(Optional.empty())
                .willReturn(Optional.of(RamenLogLike.builder().ramenLog(log).member(liker).build()));

        RamenLogLikeResponse liked = ramenLogService.toggleLike(100L, 2L);
        RamenLogLikeResponse unliked = ramenLogService.toggleLike(100L, 2L);

        assertThat(liked).isEqualTo(new RamenLogLikeResponse(true, 1));
        assertThat(unliked).isEqualTo(new RamenLogLikeResponse(false, 0));
    }

    @Test
    void rejectsUpdateByAnotherMember() {
        RamenLog log = log(100L, member(1L), true);
        given(ramenLogRepository.findByIdAndIsDeletedFalse(100L)).willReturn(Optional.of(log));

        assertThatThrownBy(() -> ramenLogService.update(100L, request(true), 2L))
                .isInstanceOf(AccessDeniedException.class);
    }

    private static RamenLogUpsertRequest request(boolean isPublic) {
        return new RamenLogUpsertRequest(
                10L,
                " 특제 돈코츠 ",
                "돈코츠",
                "proof/log.webp",
                LocalDate.of(2026, 7, 1),
                "맛있다",
                new RamenLogUpsertRequest.TasteNotesRequest(
                        List.of("진해요"),
                        List.of("단단해요"),
                        List.of("딱 좋아요"),
                        List.of("차슈 좋아요")
                ),
                RevisitIntention.DEFINITELY,
                isPublic
        );
    }

    private static MemberProfile member(Long id) {
        return MemberProfile.builder().id(id).nickname("사용자" + id).build();
    }

    private static RamenShop shop(Long id) {
        return RamenShop.builder()
                .id(id)
                .name("멘야 하루")
                .address(Address.of("서울", "마포구", "월드컵로", null))
                .build();
    }

    private static RamenLog log(Long id, MemberProfile author, boolean isPublic) {
        return RamenLog.builder()
                .id(id)
                .author(author)
                .ramenShop(shop(10L))
                .menuName("특제 돈코츠")
                .ramenType("돈코츠")
                .imageUrl("proof/log.webp")
                .revisit(RevisitIntention.DEFINITELY)
                .isPublic(isPublic)
                .build();
    }
}
