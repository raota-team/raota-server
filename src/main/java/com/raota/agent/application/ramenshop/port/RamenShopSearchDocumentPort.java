package com.raota.agent.application.ramenshop.port;

import com.raota.agent.application.ramenshop.result.RamenShopSearchDocument;
import java.util.List;

public interface RamenShopSearchDocumentPort {
    List<RamenShopSearchDocument> searchShopDocuments(String query, int topK, double similarityThreshold);
}
