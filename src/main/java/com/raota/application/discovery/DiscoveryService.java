package com.raota.application.discovery;

import com.raota.domain.member.repository.MemberRepository;
import com.raota.application.ramenShop.RamenShopViewRankingService;
import com.raota.domain.ramenShop.repository.RamenProofPictureRepository;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import com.raota.presentation.api.discovery.response.DiscoveryStatsResponse;
import com.raota.presentation.api.discovery.response.TodayPopularRamenShopResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DiscoveryService {

    private final RamenShopRepository ramenShopRepository;
    private final RamenProofPictureRepository ramenProofPictureRepository;
    private final MemberRepository memberRepository;
    private final JdbcTemplate oracleVectorJdbcTemplate;
    private final RamenShopViewRankingService ramenShopViewRankingService;

    public DiscoveryService(
            RamenShopRepository ramenShopRepository,
            RamenProofPictureRepository ramenProofPictureRepository,
            MemberRepository memberRepository,
            @Qualifier("oracleVectorJdbcTemplate") JdbcTemplate oracleVectorJdbcTemplate,
            RamenShopViewRankingService ramenShopViewRankingService
    ) {
        this.ramenShopRepository = ramenShopRepository;
        this.ramenProofPictureRepository = ramenProofPictureRepository;
        this.memberRepository = memberRepository;
        this.oracleVectorJdbcTemplate = oracleVectorJdbcTemplate;
        this.ramenShopViewRankingService = ramenShopViewRankingService;
    }

    @Transactional(readOnly = true)
    public DiscoveryStatsResponse getStats() {
        long totalShops = ramenShopRepository.count();
        long userReviews = ramenProofPictureRepository.count();
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
