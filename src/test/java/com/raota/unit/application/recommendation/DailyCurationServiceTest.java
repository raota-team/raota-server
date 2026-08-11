package com.raota.unit.application.recommendation;

import com.raota.agent.application.recommendation.DailyCurationService;
import com.raota.agent.application.recommendation.RamenRecommendationAiService;
import com.raota.agent.domain.recommendation.model.RamenType;
import com.raota.agent.domain.recommendation.model.DailyCuration;
import com.raota.agent.domain.recommendation.repository.RamenTypeRepository;
import com.raota.agent.domain.recommendation.repository.DailyCurationRepository;
import com.raota.agent.infrastructure.external.KmaWeatherClient;
import com.raota.agent.application.recommendation.dto.AiRamenRecommendationResponse;
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
class DailyCurationServiceTest {

    @Mock
    private DailyCurationRepository dailyCurationRepository;

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
    private DailyCurationService dailyCurationService;

    private RamenType mockRamenType;
    private DailyCuration mockCuration;

    @BeforeEach
    void setUp() {
        mockRamenType = RamenType.builder()
                .id(1L)
                .name("돈코츠 라멘")
                .subTitle("진한 돼지사골 육수")
                .imageUrl("tonkotsu.jpg")
                .build();

        mockCuration = DailyCuration.builder()
                .id(1L)
                .dateKey(20260624)
                .ramenType(mockRamenType)
                .title("비 오는 날의 진한 한 그릇")
                .reason("비 오는 날엔 돈코츠죠.")
                .build();
    }

    @Test
    @DisplayName("최신 큐레이션 조회 - 캐시 적중 시 캐시 데이터 반환")
    void getLatestCuration_FromCache() throws Exception {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn("cached-json");
        given(redisObjectMapper.readValue("cached-json", DailyCuration.class)).willReturn(mockCuration);

        // when
        Optional<DailyCuration> result = dailyCurationService.getLatestCuration();

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getReason()).isEqualTo("비 오는 날엔 돈코츠죠.");
        verify(dailyCurationRepository, never()).findLatest();
    }

    @Test
    @DisplayName("최신 큐레이션 조회 - 캐시 미스 시 DB 조회 및 캐싱")
    void getLatestCuration_FromDb() throws Exception {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(null);
        given(dailyCurationRepository.findLatest()).willReturn(Optional.of(mockCuration));
        given(redisObjectMapper.writeValueAsString(any())).willReturn("json-string");

        // when
        Optional<DailyCuration> result = dailyCurationService.getLatestCuration();

        // then
        assertThat(result).isPresent();
        verify(dailyCurationRepository).findLatest();
        verify(valueOperations).set(anyString(), eq("json-string"));
    }

    @Test
    @DisplayName("오늘의 큐레이션 생성 성공")
    void generateDailyCuration_Success() throws Exception {
        // given
        given(dailyCurationRepository.findByDateKey(anyInt())).willReturn(Optional.empty());
        given(weatherClient.getWeatherOutlook()).willReturn("비가 옵니다.");
        
        AiRamenRecommendationResponse aiResponse = AiRamenRecommendationResponse.builder()
                .ramenTypeName("돈코츠라멘")
                .title("추천")
                .reason("이유")
                .build();
        given(aiService.getRecommendation(anyString())).willReturn(aiResponse);
        
        given(ramenTypeRepository.findAll()).willReturn(List.of(mockRamenType));
        given(dailyCurationRepository.save(any())).willReturn(mockCuration);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisObjectMapper.writeValueAsString(any())).willReturn("json-string");

        // when
        dailyCurationService.generateDailyCuration();

        // then
        verify(weatherClient).getWeatherOutlook();
        verify(aiService).getRecommendation(anyString());
        verify(dailyCurationRepository).save(any());
        verify(valueOperations).set(anyString(), eq("json-string"));
    }
}
