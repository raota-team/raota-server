package com.raota.integration;

import com.raota.domain.recommendation.model.RamenType;
import com.raota.domain.recommendation.model.WeekendCuration;
import com.raota.domain.recommendation.repository.RamenTypeRepository;
import com.raota.domain.recommendation.repository.WeekendCurationRepository;
import com.raota.helper.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WeekendRecommendationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private RamenTypeRepository ramenTypeRepository;

    @Autowired
    private WeekendCurationRepository weekendCurationRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        RamenType tonkotsu = RamenType.builder()
                .name("돈코츠 라멘")
                .subTitle("진한 사골 육수")
                .imageUrl("http://example.com/tonkotsu.jpg")
                .build();
        RamenType savedType = ramenTypeRepository.save(tonkotsu);

        WeekendCuration curation = WeekendCuration.builder()
                .yearWeek(202625)
                .ramenType(savedType)
                .reason("테스트 추천 사유")
                .build();
        weekendCurationRepository.save(curation);
    }

    @Test
    @DisplayName("이번 주말의 라멘 추천 API 호출 성공")
    void getWeekendRecommendations_Success() throws Exception {
        mockMvc.perform(get("/api/v1/discovery/weekend-recommendations")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("돈코츠 라멘"))
                .andExpect(jsonPath("$.data[0].reason").value("테스트 추천 사유"));
    }
}
