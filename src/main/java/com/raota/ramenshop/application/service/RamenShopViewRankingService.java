package com.raota.ramenshop.application.service;

import com.raota.ramenshop.domain.repository.RamenShopRepository;
import com.raota.ramenshop.application.result.TodayPopularRamenShopResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RamenShopViewRankingService {

    private static final String KEY_PREFIX = "ramen-shop:view:";
    private static final Duration DAILY_RANKING_TTL = Duration.ofDays(2);

    private final StringRedisTemplate redisTemplate;
    private final RamenShopRepository ramenShopRepository;
    private final Clock clock;

    public void increaseTodayViewCount(Long shopId) {
        String key = todayKey();
        redisTemplate.opsForZSet().incrementScore(key, String.valueOf(shopId), 1);
        redisTemplate.expire(key, DAILY_RANKING_TTL);
    }

    public List<TodayPopularRamenShopResponse> getTodayPopularShops(int limit) {
        int size = Math.max(0, limit);
        if (size == 0) {
            return Collections.emptyList();
        }

        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(todayKey(), 0, size - 1);

        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, TodayPopularRamenShopResponse> shops = ramenShopRepository.findPopularTodayShops(shopIdsOf(tuples)).stream()
                .collect(java.util.stream.Collectors.toMap(
                        TodayPopularRamenShopResponse::ramenShopId,
                        Function.identity()
                ));

        return tuples.stream()
                .map(ZSetOperations.TypedTuple::getValue)
                .filter(Objects::nonNull)
                .map(Long::valueOf)
                .map(shops::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<Long> shopIdsOf(Set<ZSetOperations.TypedTuple<String>> tuples) {
        return tuples.stream()
                .map(ZSetOperations.TypedTuple::getValue)
                .filter(Objects::nonNull)
                .map(Long::valueOf)
                .toList();
    }

    private String todayKey() {
        return KEY_PREFIX + LocalDate.now(clock);
    }
}
