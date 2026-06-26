package com.raota.application.ramenShop.port;

import com.raota.application.ramenShop.result.RamenShopComparisonDocument;
import java.util.List;

public interface RamenShopComparisonDocumentPort {
    List<RamenShopComparisonDocument> searchComparisonDocuments(
            Long shopId,
            String query,
            int topK,
            double similarityThreshold
    );
}
