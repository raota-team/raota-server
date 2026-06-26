package com.raota.application.ramenShop.port;

import com.raota.application.ramenShop.result.RamenShopSearchDocument;
import java.util.List;

public interface RamenShopSearchDocumentPort {
    List<RamenShopSearchDocument> searchShopDocuments(String query, int topK, double similarityThreshold);
}
