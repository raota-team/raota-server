package com.raota.application.ramenShop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raota.domain.member.repository.BookmarkRepository;
import com.raota.domain.ramenShop.repository.RamenProofPictureRepository;
import com.raota.presentation.api.ramenShop.response.RamenShopBasicInfoResponse;
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
    private BookmarkRepository bookmarkRepository;

    @Mock
    private RamenProofPictureRepository ramenProofPictureRepository;

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

        given(ramenShopCacheService.getShopDetail(shopId)).willReturn(cachedResponse);
        given(bookmarkRepository.existsByMemberProfileIdAndRamenShopIdAndIsDeletedFalse(memberId, shopId))
                .willReturn(true);

        RamenShopBasicInfoResponse response = ramenShopInfoService.getShopDetailInfo(shopId, memberId);

        assertThat(response.is_bookmarked()).isTrue();
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
