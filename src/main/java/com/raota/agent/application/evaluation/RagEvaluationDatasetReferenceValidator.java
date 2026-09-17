package com.raota.agent.application.evaluation;

import com.raota.ramenshop.domain.repository.RamenShopRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Prevents a stale evaluation label from silently turning every search metric into zero.
 * Fallback and contract-only cases are deliberately excluded because they may contain
 * an intentionally missing or unsupported shop reference.
 */
@Component
public class RagEvaluationDatasetReferenceValidator {

    private final RamenShopRepository ramenShopRepository;

    public RagEvaluationDatasetReferenceValidator(RamenShopRepository ramenShopRepository) {
        this.ramenShopRepository = ramenShopRepository;
    }

    public void validate(RagEvaluationDataset dataset, RagEvaluationSplit split) {
        if (dataset == null || split == null) {
            return;
        }

        List<String> invalidReferences = new ArrayList<>();
        for (RagEvaluationCase evaluationCase : dataset.casesFor(split)) {
            if (evaluationCase.contractOnly()
                    || evaluationCase.expectsFallback()
                    || evaluationCase.expectedError() != null) {
                continue;
            }
            for (Long shopId : referencedShopIds(evaluationCase)) {
                if (shopId == null || ramenShopRepository.findByIdAndPublishedTrue(shopId).isEmpty()) {
                    invalidReferences.add(evaluationCase.caseId() + "(shopId=" + shopId + ")");
                }
            }
        }

        if (!invalidReferences.isEmpty()) {
            throw new RagEvaluationSafetyException(
                    "평가셋 " + dataset.version() + " " + split
                            + "의 매장 참조가 운영 카탈로그와 일치하지 않습니다: " + invalidReferences
            );
        }
    }

    private List<Long> referencedShopIds(RagEvaluationCase evaluationCase) {
        List<Long> shopIds = new ArrayList<>();
        switch (evaluationCase.type()) {
            case SEARCH -> evaluationCase.relevantShops().stream()
                    .map(RagExpectedShop::shopId)
                    .forEach(shopIds::add);
            case SUMMARY -> addField(shopIds, evaluationCase.request(), "shopId");
            case CHAT -> {
                JsonNode values = evaluationCase.request() == null
                        ? null : evaluationCase.request().get("shopIds");
                if (values != null && values.isArray()) {
                    values.forEach(value -> shopIds.add(toLong(value)));
                }
            }
            case COMPARE -> {
                addField(shopIds, evaluationCase.request(), "shopAId");
                addField(shopIds, evaluationCase.request(), "shopBId");
            }
        }
        return shopIds;
    }

    private void addField(List<Long> shopIds, JsonNode request, String field) {
        JsonNode value = request == null ? null : request.get(field);
        shopIds.add(toLong(value));
    }

    private Long toLong(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.longValue();
        }
        try {
            return Long.valueOf(value.asText());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
