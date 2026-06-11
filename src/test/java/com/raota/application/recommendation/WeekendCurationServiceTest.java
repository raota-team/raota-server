package com.raota.application.recommendation;

import com.raota.domain.recommendation.model.RamenType;
import com.raota.domain.recommendation.model.WeekendCuration;
import com.raota.domain.recommendation.repository.RamenTypeRepository;
import com.raota.domain.recommendation.repository.WeekendCurationRepository;
import com.raota.infrastructure.external.KmaWeatherClient;
import com.raota.application.recommendation.dto.AiRamenRecommendationResponse;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeekendCurationServiceTest {

    @Mock
    private WeekendCurationRepository weekendCurationRepository;

    @Mock
    private RamenTypeRepository ramenTypeRepository;

    @Mock
    private KmaWeatherClient weatherClient;

    @Mock
    private RamenRecommendationAiService aiService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ObjectMapper redisObjectMapper;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private WeekendCurationService weekendCurationService;

    private RamenType mockRamenType;
    private WeekendCuration mockCuration;

    @BeforeEach
    void setUp() {
        mockRamenType = RamenType.builder()
                .id(1L)
                .name("돈코츠 라멘")
                .subTitle("진한 돼지사골 육수")
                .imageUrl("tonkotsu.jpg")
                .build();

        mockCuration = WeekendCuration.builder()
                .id(1L)
                .yearWeek(202625)
                .ramenType(mockRamenType)
                .reason("비 오는 날엔 돈코츠죠.")
                .build();
    }

    @Test
    @DisplayName("최신 큐레이션 조회 - 캐시 적중 시 캐시 데이터 반환")
    void getLatestCuration_FromCache() throws Exception {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn("cached-json");
        given(redisObjectMapper.readValue("cached-json", WeekendCuration.class)).willReturn(mockCuration);

        // when
        Optional<WeekendCuration> result = weekendCurationService.getLatestCuration();

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getReason()).isEqualTo("비 오는 날엔 돈코츠죠.");
        verify(weekendCurationRepository, never()).findLatest();
    }

    @Test
    @DisplayName("최신 큐레이션 조회 - 캐시 미스 시 DB 조회 및 캐싱")
    void getLatestCuration_FromDb() throws Exception {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(null);
        given(weekendCurationRepository.findLatest()).willReturn(Optional.of(mockCuration));
        given(redisObjectMapper.writeValueAsString(any())).willReturn("json-string");

        // when
        Optional<WeekendCuration> result = weekendCurationService.getLatestCuration();

        // then
        assertThat(result).isPresent();
        verify(weekendCurationRepository).findLatest();
        verify(valueOperations).set(anyString(), eq("json-string"));
    }

    @Test
    @DisplayName("주간 큐레이션 생성 성공")
    void generateWeeklyCuration_Success() throws Exception {
        // given
        given(weekendCurationRepository.findByYearWeek(anyInt())).willReturn(Optional.empty());
        given(weatherClient.getWeatherOutlook()).willReturn("비가 옵니다.");
        
        AiRamenRecommendationResponse aiResponse = AiRamenRecommendationResponse.builder()
                .ramenTypeId("tonkotsu")
                .title("추천")
                .reason("이유")
                .build();
        given(aiService.getRecommendation(anyString())).willReturn(aiResponse);
        
        given(ramenTypeRepository.findAll()).willReturn(List.of(mockRamenType));
        given(ramenTypeRepository.findById(1L)).willReturn(Optional.of(mockRamenType));
        given(weekendCurationRepository.save(any())).willReturn(mockCuration);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisObjectMapper.writeValueAsString(any())).willReturn("json-string");

        // when
        weekendCurationService.generateWeeklyCuration();

        // then
        verify(weatherClient).getWeatherOutlook();
        verify(aiService).getRecommendation(anyString());
        verify(weekendCurationRepository).save(any());
        verify(valueOperations).set(anyString(), eq("json-string"));
    }
}
