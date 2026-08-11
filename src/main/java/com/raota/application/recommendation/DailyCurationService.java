package com.raota.application.recommendation;

import com.raota.application.recommendation.dto.AiRamenRecommendationResponse;
import com.raota.domain.recommendation.model.DailyCuration;
import com.raota.domain.recommendation.model.RamenType;
import com.raota.domain.recommendation.repository.DailyCurationRepository;
import com.raota.domain.recommendation.repository.RamenTypeRepository;
import com.raota.global.external.KmaWeatherClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Optional;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyCurationService {

    private final DailyCurationRepository dailyCurationRepository;
    private final RamenTypeRepository ramenTypeRepository;
    private final KmaWeatherClient weatherClient;
    private final RamenRecommendationAiService aiService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper redisObjectMapper;

    private static final String REDIS_KEY_LATEST_CURATION = "curation:daily:latest";

    /**
     * 오늘의 추천 정보를 조회한다.
     * Redis 캐시를 우선 조회하고, 없으면 DB에서 최신 데이터를 가져와 캐싱.
     */
    public Optional<DailyCuration> getLatestCuration() {
        try {
            String cached = redisTemplate.opsForValue().get(REDIS_KEY_LATEST_CURATION);
            if (cached != null && !cached.isBlank()) {
                return Optional.of(redisObjectMapper.readValue(cached, DailyCuration.class));
            }
        } catch (Exception e) {
            log.error("Failed to read curation from Redis cache", e);
        }

        Optional<DailyCuration> latest = dailyCurationRepository.findLatest();
        latest.ifPresent(this::cacheCuration);
        return latest;
    }

    /**
     * AI를 통해 오늘의 추천 정보를 생성한다. (배치용)
     */
    @Transactional
    public DailyCuration generateDailyCuration() {
        int currentDateKey = calculateDateKey();
        
        // 이미 오늘 데이터가 있다면 중복 생성 방지
        Optional<DailyCuration> existing = dailyCurationRepository.findByDateKey(currentDateKey);
        if (existing.isPresent()) {
            cacheCuration(existing.get());
            return existing.get();
        }

        String weatherOutlook = weatherClient.getWeatherOutlook();

        AiRamenRecommendationResponse aiResponse = aiService.getRecommendation(weatherOutlook);

        RamenType selectedType = findRamenTypeByName(aiResponse.ramenTypeName());

        DailyCuration curation = DailyCuration.builder()
                .dateKey(currentDateKey)
                .ramenType(selectedType)
                .title(defaultIfBlank(aiResponse.title(), selectedType.getName() + " 추천"))
                .reason(defaultIfBlank(aiResponse.reason(), "오늘 날씨와 잘 어울리는 라멘입니다."))
                .build();

        DailyCuration saved = dailyCurationRepository.save(curation);

        cacheCuration(saved);
        return saved;
    }

    private void cacheCuration(DailyCuration curation) {
        try {
            redisTemplate.delete(REDIS_KEY_LATEST_CURATION);
            String json = redisObjectMapper.writeValueAsString(curation);
            redisTemplate.opsForValue().set(REDIS_KEY_LATEST_CURATION, json);
        } catch (Exception e) {
            log.error("Failed to cache curation to Redis", e);
        }
    }

    private int calculateDateKey() {
        LocalDate now = LocalDate.now();
        return Integer.parseInt(now.toString().replace("-", ""));
    }

    private RamenType findRamenTypeByName(String name) {
        String targetName = defaultIfBlank(name, "").replace(" ", "").toLowerCase();
        
        return ramenTypeRepository.findAll().stream()
                .filter(t -> {
                    String dbName = t.getName().replace(" ", "").toLowerCase();
                    return dbName.contains(targetName) || targetName.contains(dbName);
                })
                .findFirst()
                .orElseGet(() -> ramenTypeRepository.findById(1L).orElseThrow());
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
