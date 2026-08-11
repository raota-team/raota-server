package com.raota.discovery.application;

import com.raota.account.domain.member.repository.MemberRepository;
import com.raota.ramenshop.application.service.RamenShopViewRankingService;
import com.raota.ramenlog.domain.repository.RamenLogRepository;
import com.raota.ramenshop.domain.repository.RamenShopRepository;
import com.raota.discovery.presentation.response.DiscoveryStatsResponse;
import com.raota.ramenshop.application.result.TodayPopularRamenShopResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DiscoveryService {

    private final RamenShopRepository ramenShopRepository;
    private final RamenLogRepository ramenLogRepository;
    private final MemberRepository memberRepository;
    private final JdbcTemplate oracleVectorJdbcTemplate;
    private final RamenShopViewRankingService ramenShopViewRankingService;

    public DiscoveryService(
            RamenShopRepository ramenShopRepository,
            RamenLogRepository ramenLogRepository,
            MemberRepository memberRepository,
            @Qualifier("oracleVectorJdbcTemplate") JdbcTemplate oracleVectorJdbcTemplate,
            RamenShopViewRankingService ramenShopViewRankingService
    ) {
        this.ramenShopRepository = ramenShopRepository;
        this.ramenLogRepository = ramenLogRepository;
        this.memberRepository = memberRepository;
        this.oracleVectorJdbcTemplate = oracleVectorJdbcTemplate;
        this.ramenShopViewRankingService = ramenShopViewRankingService;
    }

    @Transactional(readOnly = true)
    public DiscoveryStatsResponse getStats() {
        long totalShops = ramenShopRepository.countByPublishedTrue();
        long userReviews = ramenLogRepository.countByIsDeletedFalse();
        long totalUsers = 4000;

        long externalReviews = 0;
        try {
            Long count = oracleVectorJdbcTemplate.queryForObject("SELECT count(*) FROM SPRING_AI_VECTORS", Long.class);
            if (count != null) {
                externalReviews = count;
            }
        } catch (Exception e) {
        }

        return new DiscoveryStatsResponse(totalShops, userReviews + externalReviews, totalUsers);
    }

    public List<TodayPopularRamenShopResponse> getTodayPopularShops(int limit) {
        return ramenShopViewRankingService.getTodayPopularShops(limit);
    }
}
