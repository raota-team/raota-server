package com.raota.unit.domain.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import com.raota.domain.retrieval.document.RetrievalDocumentFilters;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.oracle.SqlJsonPathFilterExpressionConverter;

class RetrievalDocumentFiltersTest {

    private final SqlJsonPathFilterExpressionConverter converter = new SqlJsonPathFilterExpressionConverter();

    @Test
    void externalReviewChunksForShop_should_scope_external_review_sources_to_shop_id() {
        String expression = converter.convertExpression(RetrievalDocumentFilters.externalReviewChunksForShop(1L));

        assertThat(expression)
                .contains("@.\"shopId\" == \"1\"")
                .contains("@.\"documentType\" == \"EXTERNAL_REVIEW_CHUNK\"")
                .contains("@.\"source\" in ( \"CATCHTABLE\",\"NAVER_REVIEW\" )")
                .contains("@.\"shopId\" == \"1\" && (@.\"documentType\" == \"EXTERNAL_REVIEW_CHUNK\"");
    }

    @Test
    void shopProfileOrExternalReviewsForShop_should_group_profile_or_external_reviews_under_shop_id() {
        String expression = converter.convertExpression(RetrievalDocumentFilters.shopProfileOrExternalReviewsForShop(1L));

        assertThat(expression)
                .contains("@.\"shopId\" == \"1\"")
                .contains("(@.\"documentType\" == \"SHOP_PROFILE\" ||")
                .contains("(@.\"documentType\" == \"EXTERNAL_REVIEW_CHUNK\" && @.\"source\" in ( \"CATCHTABLE\",\"NAVER_REVIEW\" ))");
    }
}
