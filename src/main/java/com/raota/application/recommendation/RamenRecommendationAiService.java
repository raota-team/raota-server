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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class RamenRecommendationAiService {
    private static final Pattern THINK_BLOCK_PATTERN = Pattern.compile("(?s)<think>.*?</think>");
    private static final Pattern JSON_CODE_BLOCK_PATTERN = Pattern.compile("(?s)```json\\s*(\\{.*?})\\s*```");
    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("(?s)(\\{.*})");

    private final ChatModel chatModel;
    private final ObjectMapper redisObjectMapper;

    @Value("classpath:/prompts/today-ramen-recommendation.st")
    private Resource promptResource;

    private static final List<String> RAMEN_NAMES = List.of(
            "시오라멘", "쇼유라멘", "아부라소바", "마제소바", "츠케멘", "이에케라멘",
            "돈코츠라멘", "토리파이탄", "미소라멘", "쇼유파이탄", "토마토라멘",
            "차슈멘", "탄탄멘", "중화소바"
    );

    public AiRamenRecommendationResponse getRecommendation(String weatherOutlook) {
        PromptTemplate template = new PromptTemplate(promptResource);
        template.add("weatherOutlook", weatherOutlook);
        template.add("ramenNames", String.join(", ", RAMEN_NAMES));

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
            String cleanedJson = extractJsonPayload(json);
            return redisObjectMapper.readValue(cleanedJson, AiRamenRecommendationResponse.class);
        } catch (Exception e) {
            log.error("JSON Parsing Error. Raw response: {}", json, e);
            return fallback();
        }
    }

    private String extractJsonPayload(String response) {
        if (response == null) {
            return "";
        }

        String withoutThink = THINK_BLOCK_PATTERN.matcher(response).replaceAll("").trim();

        Matcher jsonCodeBlockMatcher = JSON_CODE_BLOCK_PATTERN.matcher(withoutThink);
        if (jsonCodeBlockMatcher.find()) {
            return jsonCodeBlockMatcher.group(1).trim();
        }

        Matcher jsonObjectMatcher = JSON_OBJECT_PATTERN.matcher(withoutThink);
        if (jsonObjectMatcher.find()) {
            return jsonObjectMatcher.group(1).trim();
        }

        return withoutThink.replaceAll("```json|```", "").trim();
    }

    private AiRamenRecommendationResponse fallback() {
        return AiRamenRecommendationResponse.builder()
                .ramenTypeName("돈코츠라멘")
                .title("언제나 든든한 돈코츠 라멘")
                .reason("기상 정보를 분석하는 중 문제가 발생했지만, 든든한 돈코츠 라멘은 언제나 최고의 선택입니다.")
                .build();
    }
}
