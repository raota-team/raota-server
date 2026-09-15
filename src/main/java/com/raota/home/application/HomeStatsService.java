package com.raota.home.application;

import com.raota.agent.application.retrieval.RetrievalStatisticsService;
import com.raota.home.presentation.response.HomeStatsResponse;
import com.raota.ramenlog.application.RamenLogStatisticsService;
import com.raota.ramenshop.application.service.RamenShopStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HomeStatsService {

    private static final long LEGACY_DISPLAY_USER_COUNT = 4_000;

    private final RamenShopStatisticsService ramenShopStatisticsService;
    private final RamenLogStatisticsService ramenLogStatisticsService;
    private final RetrievalStatisticsService retrievalStatisticsService;

    public HomeStatsResponse getStats() {
        long totalShops = ramenShopStatisticsService.countPublishedShops();
        long userReviews = ramenLogStatisticsService.countActiveLogs();
        long indexedDocuments = retrievalStatisticsService.countIndexedDocuments();

        return new HomeStatsResponse(
                totalShops,
                userReviews + indexedDocuments,
                LEGACY_DISPLAY_USER_COUNT
        );
    }
}
