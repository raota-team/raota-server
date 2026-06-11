package com.raota.application.recommendation;

import com.raota.application.recommendation.dto.AiRamenRecommendationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RamenRecommendationAiService {

    private final ChatModel chatModel;
    private final ObjectMapper redisObjectMapper;

    @Value("classpath:/prompts/weekend-ramen-recommendation.st")
    private Resource promptResource;

    private static final List<String> RAMEN_TYPES = List.of(
            "sio", "shoyu", "aburasoba", "mazesoba", "tsukemen", "iekei",
            "tonkotsu", "toripaitan", "miso", "shoyupaitan", "tomato",
            "chashumen", "tantanmen", "chuukasoba"
    );

    public AiRamenRecommendationResponse getRecommendation(String weatherOutlook) {
        PromptTemplate template = new PromptTemplate(promptResource);
        template.add("weatherOutlook", weatherOutlook);
        template.add("ramenTypes", String.join(", ", RAMEN_TYPES));

        try {
            String responseJson = ChatClient.create(chatModel).prompt(template.create())
                    .call()
                    .content();

            return parseResponse(responseJson);
        } catch (Exception e) {
            log.error("Failed to get AI recommendation", e);
            return fallback();
        }
    }

    private AiRamenRecommendationResponse parseResponse(String json) {
        try {
            String cleanedJson = json.replaceAll("```json|```", "").trim();
            return redisObjectMapper.readValue(cleanedJson, AiRamenRecommendationResponse.class);
        } catch (Exception e) {
            log.error("JSON Parsing Error: {}", json, e);
            return fallback();
        }
    }

    private AiRamenRecommendationResponse fallback() {
        return AiRamenRecommendationResponse.builder()
                .ramenTypeId("tonkotsu")
                .title("언제나 든든한 돈코츠 라멘")
                .reason("기상 정보를 분석하는 중 문제가 발생했지만, 든든한 돈코츠 라멘은 언제나 최고의 선택입니다.")
                .build();
    }
}
