package com.raota.agent.application.ramenshop.port;

import com.raota.agent.application.ramenshop.result.RamenShopComparisonDocument;
import java.util.List;

public interface RamenShopComparisonDocumentPort {
    List<RamenShopComparisonDocument> searchComparisonDocuments(
            Long shopId,
            String query,
            int topK,
            double similarityThreshold
    );
}
