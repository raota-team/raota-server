package com.raota.acceptance.api;

import com.raota.domain.recommendation.model.RamenType;
import com.raota.domain.recommendation.model.DailyCuration;
import com.raota.domain.recommendation.repository.RamenTypeRepository;
import com.raota.domain.recommendation.repository.DailyCurationRepository;
import com.raota.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TodayRecommendationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private RamenTypeRepository ramenTypeRepository;

    @Autowired
    private DailyCurationRepository dailyCurationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        redisTemplate.delete("curation:daily:latest");
        jdbcTemplate.update("DELETE FROM tb_daily_curation");
        jdbcTemplate.update("DELETE FROM tb_ramen_type");

        RamenType tonkotsu = RamenType.builder()
                .name("돈코츠 라멘")
                .subTitle("진한 사골 육수")
                .imageUrl("http://example.com/tonkotsu.jpg")
                .build();
        RamenType savedType = ramenTypeRepository.save(tonkotsu);

        DailyCuration curation = DailyCuration.builder()
                .dateKey(currentDateKey())
                .ramenType(savedType)
                .title("비 오는 오늘의 진한 한 그릇")
                .reason("테스트 추천 사유")
                .build();
        dailyCurationRepository.save(curation);
    }

    @Test
    @DisplayName("오늘의 라멘 추천 API 호출 성공")
    void getTodayRecommendations_Success() throws Exception {
        mockMvc.perform(get("/api/v1/discovery/today-recommendations")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("돈코츠 라멘"))
                .andExpect(jsonPath("$.data[0].title").value("비 오는 오늘의 진한 한 그릇"))
                .andExpect(jsonPath("$.data[0].reason").value("테스트 추천 사유"));
    }

    @Test
    @DisplayName("오늘의 라멘 추천 수동 생성 API 호출 성공")
    void generateTodayRecommendations_Success() throws Exception {
        mockMvc.perform(post("/api/v1/discovery/today-recommendations/generate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("돈코츠 라멘"))
                .andExpect(jsonPath("$.data.title").value("비 오는 오늘의 진한 한 그릇"))
                .andExpect(jsonPath("$.data.reason").value("테스트 추천 사유"));
    }

    private int currentDateKey() {
        return Integer.parseInt(LocalDate.now().toString().replace("-", ""));
    }
}
