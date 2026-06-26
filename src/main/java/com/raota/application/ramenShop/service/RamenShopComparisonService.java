package com.raota.application.ramenShop.service;

import com.raota.application.ramenShop.port.RamenShopComparisonDocumentPort;
import com.raota.application.ramenShop.port.RamenShopComparisonNarrativePort;
import com.raota.application.ramenShop.result.AiRamenShopComparisonResult;
import com.raota.application.ramenShop.result.RamenShopComparisonDocument;
import com.raota.application.ramenShop.result.RamenShopComparisonResult;
import com.raota.application.ramenShop.search.RamenShopComparisonSearchPolicy;
import com.raota.application.ramenShop.search.RamenShopReader;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.retrieval.document.RetrievalMetadataKeys;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RamenShopComparisonService {

    private final RamenShopReader ramenShopReader;
    private final RamenShopComparisonDocumentPort comparisonDocumentPort;
    private final RamenShopComparisonNarrativePort comparisonNarrativePort;
    private final RamenShopComparisonSearchPolicy comparisonSearchPolicy;

    public RamenShopComparisonService(
            RamenShopReader ramenShopReader,
            RamenShopComparisonDocumentPort comparisonDocumentPort,
            RamenShopComparisonNarrativePort comparisonNarrativePort,
            RamenShopComparisonSearchPolicy comparisonSearchPolicy
    ) {
        this.ramenShopReader = ramenShopReader;
        this.comparisonDocumentPort = comparisonDocumentPort;
        this.comparisonNarrativePort = comparisonNarrativePort;
        this.comparisonSearchPolicy = comparisonSearchPolicy;
    }

    public RamenShopComparisonResult compareShops(Long shopAId, Long shopBId, String focus) {
        validateComparisonRequest(shopAId, shopBId);

        RamenShop shopA = ramenShopReader.getRamenShop(shopAId);
        RamenShop shopB = ramenShopReader.getRamenShop(shopBId);
        String normalizedFocus = comparisonSearchPolicy.normalizeFocus(focus);

        List<RamenShopComparisonDocument> shopADocuments = collectComparisonDocuments(shopA, normalizedFocus);
        List<RamenShopComparisonDocument> shopBDocuments = collectComparisonDocuments(shopB, normalizedFocus);

        if (hasInsufficientDocuments(shopADocuments, shopBDocuments)) {
            return buildComparisonResult(shopA, shopB, normalizedFocus, null);
        }

        String contextA = buildComparisonContext(shopA, shopADocuments);
        String contextB = buildComparisonContext(shopB, shopBDocuments);
        AiRamenShopComparisonResult aiResult = comparisonNarrativePort.generateComparisonNarratives(
                normalizedFocus,
                contextA,
                contextB
        );

        return buildComparisonResult(shopA, shopB, normalizedFocus, aiResult);
    }

    private void validateComparisonRequest(Long shopAId, Long shopBId) {
        if (shopAId == null) {
            throw new IllegalArgumentException("비교할 첫 번째 매장 ID는 필수입니다.");
        }
        if (shopBId == null) {
            throw new IllegalArgumentException("비교할 두 번째 매장 ID는 필수입니다.");
        }
        if (shopAId.equals(shopBId)) {
            throw new IllegalArgumentException("서로 다른 두 매장을 선택해야 합니다.");
        }
    }

    private RamenShopComparisonResult buildComparisonResult(
            RamenShop shopA,
            RamenShop shopB,
            String focus,
            AiRamenShopComparisonResult aiResult
    ) {
        return new RamenShopComparisonResult(
                new RamenShopComparisonResult.ShopSummary(shopA.getId(), shopA.getName()),
                new RamenShopComparisonResult.ShopSummary(shopB.getId(), shopB.getName()),
                focus,
                toNarratives(aiResult)
        );
    }

    private boolean hasInsufficientDocuments(
            List<RamenShopComparisonDocument> shopADocuments,
            List<RamenShopComparisonDocument> shopBDocuments
    ) {
        return shopADocuments == null || shopADocuments.isEmpty()
                || shopBDocuments == null || shopBDocuments.isEmpty();
    }

    private List<RamenShopComparisonResult.ComparisonNarrative> toNarratives(AiRamenShopComparisonResult aiResult) {
        if (aiResult == null || aiResult.narratives() == null || aiResult.narratives().isEmpty()) {
            return fallbackNarratives();
        }

        List<RamenShopComparisonResult.ComparisonNarrative> narratives = aiResult.narratives().stream()
                .filter(Objects::nonNull)
                .filter(narrative -> ramenShopReader.hasText(narrative.title())
                        && ramenShopReader.hasText(narrative.body()))
                .map(narrative -> new RamenShopComparisonResult.ComparisonNarrative(
                        narrative.title().trim(),
                        narrative.body().trim()
                ))
                .toList();

        if (narratives.isEmpty()) {
            return fallbackNarratives();
        }

        return narratives;
    }

    private List<RamenShopComparisonResult.ComparisonNarrative> fallbackNarratives() {
        return List.of(new RamenShopComparisonResult.ComparisonNarrative(
                "비교 정보 부족",
                "두 매장을 객관적으로 비교할 수 있는 검색 문서가 충분하지 않습니다. 리뷰나 매장 프로필 데이터가 쌓인 뒤 다시 비교해 주세요."
        ));
    }

    private List<RamenShopComparisonDocument> collectComparisonDocuments(RamenShop shop, String focus) {
        String query = comparisonSearchPolicy.buildQuery(shop, focus);

        return comparisonDocumentPort.searchComparisonDocuments(
                shop.getId(),
                query,
                comparisonSearchPolicy.documentLimit(),
                comparisonSearchPolicy.similarityThreshold()
        );
    }

    private String buildComparisonContext(RamenShop shop, List<RamenShopComparisonDocument> documents) {
        String documentContext = buildDocumentContext(documents);

        return """
            매장명: %s
            주소: %s
            태그: %s
            설명: %s

            [검색된 문서]
            %s
            """.formatted(
                shop.getName(),
                ramenShopReader.addressTextOrDefault(shop),
                ramenShopReader.tagsTextOrDefault(shop),
                ramenShopReader.descriptionTextOrDefault(shop),
                documentContext
        );
    }

    private String buildDocumentContext(List<RamenShopComparisonDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return "검색된 리뷰/프로필 문서가 부족합니다.";
        }

        return documents.stream()
                .map(this::formatDocument)
                .collect(Collectors.joining("\n"));
    }

    private String formatDocument(RamenShopComparisonDocument document) {
        Object documentType = document.metadata().get(RetrievalMetadataKeys.DOCUMENT_TYPE);
        Object source = document.metadata().get(RetrievalMetadataKeys.SOURCE);

        return """
            - 문서유형: %s
              출처: %s
              내용: %s
            """.formatted(
                documentType == null ? "UNKNOWN" : documentType,
                source == null ? "UNKNOWN" : source,
                document.text()
        );
    }
}
