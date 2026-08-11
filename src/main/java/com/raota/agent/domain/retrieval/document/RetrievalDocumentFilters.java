package com.raota.agent.domain.retrieval.document;

import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

public final class RetrievalDocumentFilters {

    private RetrievalDocumentFilters() {
    }

    public static Filter.Expression externalReviewChunksForShop(Long shopId) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();

        return builder.and(
                shopIdFilter(builder, shopId),
                builder.group(builder.and(
                        externalReviewChunkFilter(builder),
                        externalReviewSourceFilter(builder)
                ))
        ).build();
    }

    public static Filter.Expression shopProfileOrExternalReviewsForShop(Long shopId) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();

        return builder.and(
                shopIdFilter(builder, shopId),
                builder.group(builder.or(
                        shopProfileFilter(builder),
                        builder.group(builder.and(
                                externalReviewChunkFilter(builder),
                                externalReviewSourceFilter(builder)
                        ))
                ))
        ).build();
    }

    private static FilterExpressionBuilder.Op shopIdFilter(FilterExpressionBuilder builder, Long shopId) {
        return builder.eq(RetrievalMetadataKeys.SHOP_ID, String.valueOf(shopId));
    }

    private static FilterExpressionBuilder.Op shopProfileFilter(FilterExpressionBuilder builder) {
        return builder.eq(RetrievalMetadataKeys.DOCUMENT_TYPE, RetrievalDocumentType.SHOP_PROFILE.name());
    }

    private static FilterExpressionBuilder.Op externalReviewChunkFilter(FilterExpressionBuilder builder) {
        return builder.eq(RetrievalMetadataKeys.DOCUMENT_TYPE, RetrievalDocumentType.EXTERNAL_REVIEW_CHUNK.name());
    }

    private static FilterExpressionBuilder.Op externalReviewSourceFilter(FilterExpressionBuilder builder) {
        return builder.in(
                RetrievalMetadataKeys.SOURCE,
                RetrievalDocumentSource.CATCHTABLE.name(),
                RetrievalDocumentSource.NAVER_REVIEW.name()
        );
    }
}
