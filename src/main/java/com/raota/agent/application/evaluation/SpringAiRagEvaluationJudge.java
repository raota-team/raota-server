package com.raota.agent.application.evaluation;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/** Optional structured LLM judge. The deterministic judge remains the default. */
@Service
@Primary
@ConditionalOnProperty(name = "app.rag.evaluation.llm-judge.enabled", havingValue = "true")
public class SpringAiRagEvaluationJudge implements RagEvaluationJudge {

    private final ChatClient chatClient;

    private final ObjectMapper objectMapper;

    private final Resource prompt;

    public SpringAiRagEvaluationJudge(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper,
            @Value("classpath:/prompts/rag-evaluation-judge.st") Resource prompt) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
        this.prompt = prompt;
    }

    @Override
    public RagEvaluationJudgeResult suggest(RagEvaluationCase evaluationCase, RagExecutionResult executionResult) {
        try {
            RagEvaluationJudgeResult result = chatClient.prompt()
                .user(user -> user.text(prompt)
                    .param("caseType", evaluationCase.type().name())
                    .param("request", toJson(evaluationCase.request()))
                    .param("expected", toJson(expectedForJudge(evaluationCase)))
                    .param("response", toJson(executionResult.response()))
                    .param("evidence", toJson(executionResult.evidence())))
                .call()
                .entity(RagEvaluationJudgeResult.class);
            return new RagEvaluationJudgeResult(result == null ? 0 : result.groundedness(),
                    result == null ? java.util.List.of() : result.unsupportedClaims(),
                    result != null && result.informationSufficiencyCorrect(),
                    result == null ? "REVIEW" : result.verdict().toUpperCase(Locale.ROOT),
                    result == null ? "구조화된 평가 결과가 없어 검수가 필요합니다." : result.reason(), "llm-rag-judge-v1");
        }
        catch (RuntimeException exception) {
            return new RagEvaluationJudgeResult(0, java.util.List.of(), false, "REVIEW",
                    "LLM 자동 판정에 실패하여 사람 검수가 필요합니다: " + exception.getMessage(), "llm-rag-judge-error");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        }
        catch (JacksonException exception) {
            return "{}";
        }
    }

    private Map<String, Object> expectedForJudge(RagEvaluationCase evaluationCase) {
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("relevantShops", evaluationCase.relevantShops());
        expected.put("requiredFacts", evaluationCase.requiredFacts());
        expected.put("allowedEvidence", evaluationCase.allowedEvidence());
        expected.put("forbiddenClaims", evaluationCase.forbiddenClaims());
        expected.put("expectsFallback", evaluationCase.expectsFallback());
        expected.put("primaryK", evaluationCase.primaryK());
        expected.put("diagnosticK", evaluationCase.diagnosticK());
        return expected;
    }

}
