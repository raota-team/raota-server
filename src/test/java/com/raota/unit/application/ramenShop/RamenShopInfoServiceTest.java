package com.raota.unit.application.ramenShop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raota.application.ramenShop.service.RamenShopCacheService;
import com.raota.application.ramenShop.service.RamenShopInfoService;
import com.raota.application.ramenShop.service.RamenShopViewRankingService;
import com.raota.domain.member.repository.BookmarkRepository;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.ramenlog.repository.RamenLogRepository;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import com.raota.infrastructure.cache.CacheInvalidationPublisher;
import com.raota.presentation.api.ramenShop.response.RamenShopBasicInfoResponse;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RamenShopInfoServiceTest {

    @Mock
    private RamenShopCacheService ramenShopCacheService;

    @Mock
    private RamenShopRepository ramenShopRepository;

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private RamenLogRepository ramenLogRepository;

    @Mock
    private CacheInvalidationPublisher cacheInvalidationPublisher;

    @Mock
    private RamenShopViewRankingService ramenShopViewRankingService;

    @InjectMocks
    private RamenShopInfoService ramenShopInfoService;

    @Test
    void getShopDetailInfoReflectsBookmarkStatusForLoginMember() {
        Long shopId = 1L;
        Long memberId = 10L;
        RamenShopBasicInfoResponse cachedResponse = RamenShopBasicInfoResponse.builder()
                .id(shopId)
                .name("멘야 하쿠")
                .is_bookmarked(false)
                .build();

        given(ramenShopRepository.findByIdAndPublishedTrue(shopId)).willReturn(Optional.of(RamenShop.builder()
                .name("멘야 하쿠")
                .build()));
        given(ramenShopCacheService.getShopDetail(shopId)).willReturn(cachedResponse);
        given(bookmarkRepository.existsByMemberProfileIdAndRamenShopIdAndIsDeletedFalse(memberId, shopId))
                .willReturn(true);

        RamenShopBasicInfoResponse response = ramenShopInfoService.getShopDetailInfo(shopId, memberId);

        assertThat(response.is_bookmarked()).isTrue();
    }

    @Test
    void increaseViewCountUpdatesShopAndRanking() {
        Long shopId = 1L;
        RamenShop shop = RamenShop.builder()
                .name("멘야 하쿠")
                .build();

        given(ramenShopRepository.findByIdAndPublishedTrue(shopId)).willReturn(Optional.of(shop));

        ramenShopInfoService.increaseViewCount(shopId);

        assertThat(shop.getStats().viewCount()).isEqualTo(1);
        verify(ramenShopViewRankingService).increaseTodayViewCount(shopId);
        verify(cacheInvalidationPublisher).publish("ramenShopDetail", String.valueOf(shopId));
        verify(cacheInvalidationPublisher).publishAll("ramenShopList");
    }

    @Test
    void shopDetailResponseSerializesSnakeCaseBookmarkFieldOnly() throws Exception {
        RamenShopBasicInfoResponse response = RamenShopBasicInfoResponse.builder()
                .id(1L)
                .name("멘야 하쿠")
                .is_bookmarked(true)
                .build();

        JsonNode json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(response));

        assertThat(json.get("is_bookmarked").asBoolean()).isTrue();
        assertThat(json.has("isBookmarked")).isFalse();
    }
}
