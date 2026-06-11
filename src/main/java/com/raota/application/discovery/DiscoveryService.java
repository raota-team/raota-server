package com.raota.application.discovery;

import com.raota.domain.member.repository.MemberRepository;
import com.raota.domain.ramenShop.repository.RamenProofPictureRepository;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import com.raota.presentation.api.discovery.response.DiscoveryStatsResponse;
import com.raota.presentation.api.discovery.response.TrendingTagResponse;
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

    public DiscoveryService(
            RamenShopRepository ramenShopRepository,
            RamenProofPictureRepository ramenProofPictureRepository,
            MemberRepository memberRepository,
            @Qualifier("oracleVectorJdbcTemplate") JdbcTemplate oracleVectorJdbcTemplate
    ) {
        this.ramenShopRepository = ramenShopRepository;
        this.ramenProofPictureRepository = ramenProofPictureRepository;
        this.memberRepository = memberRepository;
        this.oracleVectorJdbcTemplate = oracleVectorJdbcTemplate;
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

    public List<TrendingTagResponse> getTrendingTags(int limit) {
        // TODO: Implement real trending logic with search history
        return List.of(
                new TrendingTagResponse(1, "토리파이탄", "up"),
                new TrendingTagResponse(2, "이에케", "same"),
                new TrendingTagResponse(3, "혼밥 맛집", "new"),
                new TrendingTagResponse(4, "마포구", "same"),
                new TrendingTagResponse(5, "츠케멘", "up")
        ).subList(0, Math.min(limit, 5));
    }
}
