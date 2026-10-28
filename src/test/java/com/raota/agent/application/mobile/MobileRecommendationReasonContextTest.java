package com.raota.agent.application.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.raota.mobile.shop.application.facade.MobileShopAgentFacade;
import com.raota.mobile.shop.application.facade.MobileShopAgentFacade.MobileShopReasonFacts;
import com.raota.mobile.shop.application.result.MobileShopSummaryResult;
import com.raota.mobile.shop.domain.model.MobileShopBusinessStatus;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** LLM에 넘기는 추천 이유 근거의 거리·가격 표기다. 모델은 받은 표기를 그대로 옮기므로 사람이 읽는 형태로 넘긴다. */
class MobileRecommendationReasonContextTest {

    private final MobileShopAgentFacade shops = mock(MobileShopAgentFacade.class);

    private final MobileRecommendationService service =
            new MobileRecommendationService(null, shops, null, null, null, null, null, Duration.ofSeconds(20));

    @ParameterizedTest
    @CsvSource({
        "104, 100m",
        "383, 380m",
        "995, 1km",
        "999, 1km",
        "1000, 1km",
        "2000, 2km",
        "2049, 2km",
        "15396, 15.4km",
        "100000, 100km"
    })
    void 거리는_1km_미만이면_10m_단위_이상이면_km_소수_첫째_자리다(int meters, String expected) {
        assertThat(MobileRecommendationService.distanceLabel(meters)).isEqualTo(expected);
    }

    @Test
    void 근거에_가까운_거리는_읽기_쉽게_넣고_3km를_넘거나_없으면_거리_줄을_뺀다() {
        when(shops.reasonFacts(any())).thenReturn(Map.of());

        String context =
                service.reasonContext(List.of(shop("1", 104), shop("2", 3_000), shop("3", 3_001), shop("4", null)));

        assertThat(context)
                .contains("- 매장 ID: 1\n  거리: 100m")
                .contains("- 매장 ID: 2\n  거리: 3km")
                .doesNotContainPattern("매장 ID: 3\\n  거리")
                .doesNotContainPattern("매장 ID: 4\\n  거리");
        assertThat(context.lines().filter(line -> line.startsWith("  거리")).count())
                .isEqualTo(2);
    }

    @Test
    void 가격은_천_단위_쉼표를_넣고_같은_값이면_하나만_쓴다() {
        when(shops.reasonFacts(any()))
                .thenReturn(Map.of(
                        1L, new MobileShopReasonFacts(null, null, 8_000, 10_000, List.of()),
                        2L, new MobileShopReasonFacts(null, null, 9_000, 9_000, List.of()),
                        3L, new MobileShopReasonFacts(null, null, null, 12_000, List.of())));

        String context = service.reasonContext(List.of(shop("1", null), shop("2", null), shop("3", null)));

        assertThat(context)
                .contains("- 매장 ID: 1\n  가격대: 8,000~10,000원")
                .contains("- 매장 ID: 2\n  가격대: 9,000원")
                .contains("- 매장 ID: 3\n  가격대: 12,000원");
    }

    private static MobileShopSummaryResult shop(String id, Integer distanceMeters) {
        return new MobileShopSummaryResult(
                id,
                "매장 " + id,
                null,
                "서울",
                "마포구",
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                0,
                0,
                false,
                MobileShopBusinessStatus.OPERATIONAL,
                null,
                distanceMeters);
    }
}
