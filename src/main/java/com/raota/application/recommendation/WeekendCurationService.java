package com.raota.application.recommendation;

import com.raota.application.recommendation.dto.AiRamenRecommendationResponse;
import com.raota.domain.recommendation.model.RamenType;
import com.raota.domain.recommendation.model.WeekendCuration;
import com.raota.domain.recommendation.repository.RamenTypeRepository;
import com.raota.domain.recommendation.repository.WeekendCurationRepository;
import com.raota.infrastructure.external.KmaWeatherClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.Optional;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeekendCurationService {

    private final WeekendCurationRepository weekendCurationRepository;
    private final RamenTypeRepository ramenTypeRepository;
    private final KmaWeatherClient weatherClient;
    private final RamenRecommendationAiService aiService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper redisObjectMapper;

    private static final String REDIS_KEY_LATEST_CURATION = "curation:weekend:latest";

    /**
     * 이번 주말의 추천 정보를 조회한다.
     * Redis 캐시를 우선 조회하고, 없으면 DB에서 최신 데이터를 가져와 캐싱.
     */
    public Optional<WeekendCuration> getLatestCuration() {
        try {
            String cached = redisTemplate.opsForValue().get(REDIS_KEY_LATEST_CURATION);
            if (cached != null && !cached.isBlank()) {
                return Optional.of(redisObjectMapper.readValue(cached, WeekendCuration.class));
            }
        } catch (Exception e) {
            log.error("Failed to read curation from Redis cache", e);
        }

        Optional<WeekendCuration> latest = weekendCurationRepository.findLatest();
        latest.ifPresent(this::cacheCuration);
        return latest;
    }

    /**
     * AI를 통해 이번 주 주간 추천 정보를 생성한다. (배치용)
     */
    @Transactional
    public void generateWeeklyCuration() {
        int currentYearWeek = calculateYearWeek();
        
        // 이미 해당 주차에 데이터가 있다면 중복 생성 방지
        if (weekendCurationRepository.findByYearWeek(currentYearWeek).isPresent()) {
            return;
        }

        String weatherOutlook = weatherClient.getWeatherOutlook();

        AiRamenRecommendationResponse aiResponse = aiService.getRecommendation(weatherOutlook);

        RamenType selectedType = findRamenTypeByAiResponse(aiResponse.ramenTypeId());

        WeekendCuration curation = WeekendCuration.builder()
                .yearWeek(currentYearWeek)
                .ramenType(selectedType)
                .reason(aiResponse.reason())
                .build();

        WeekendCuration saved = weekendCurationRepository.save(curation);

        cacheCuration(saved);
    }

    private void cacheCuration(WeekendCuration curation) {
        try {
            String json = redisObjectMapper.writeValueAsString(curation);
            redisTemplate.opsForValue().set(REDIS_KEY_LATEST_CURATION, json);
        } catch (Exception e) {
            log.error("Failed to cache curation to Redis", e);
        }
    }

    private int calculateYearWeek() {
        LocalDate now = LocalDate.now();
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        int weekNumber = now.get(weekFields.weekOfWeekBasedYear());
        return (now.getYear() * 100) + weekNumber;
    }

    private RamenType findRamenTypeByAiResponse(String typeId) {
        // AI가 반환한 ID가 DB에 어떤 형태로 저장되어 있는지에 따라 달라짐.
        // 현재 tb_ramen_type.id는 Long이므로, 텍스트(idString)를 통해 매칭 시도.
        // 만약 카테고리명(name)에 해당 텍스트가 포함되어 있다면 해당 타입 선택.
        return ramenTypeRepository.findAll().stream()
                .filter(t -> t.getName().toLowerCase().replace(" ", "").contains(typeId.toLowerCase()))
                .findFirst()
                .orElseGet(() -> ramenTypeRepository.findById(1L).orElseThrow());
    }
}
