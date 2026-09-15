package com.raota.home.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.raota.agent.application.retrieval.statistics.RetrievalStatisticsService;
import com.raota.home.presentation.response.HomeStatsResponse;
import com.raota.ramenlog.application.statistics.RamenLogStatisticsService;
import com.raota.ramenshop.application.service.RamenShopStatisticsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HomeStatsServiceTest {

    @Mock
    private RamenShopStatisticsService ramenShopStatisticsService;

    @Mock
    private RamenLogStatisticsService ramenLogStatisticsService;

    @Mock
    private RetrievalStatisticsService retrievalStatisticsService;

    @InjectMocks
    private HomeStatsService homeStatsService;

    @Test
    void combines_legacy_home_statistics_without_changing_the_response_contract() {
        given(ramenShopStatisticsService.countPublishedShops()).willReturn(12L);
        given(ramenLogStatisticsService.countActiveLogs()).willReturn(30L);
        given(retrievalStatisticsService.countIndexedDocuments()).willReturn(70L);

        HomeStatsResponse result = homeStatsService.getStats();

        assertThat(result).isEqualTo(new HomeStatsResponse(12L, 100L, 4_000L));
    }
}
