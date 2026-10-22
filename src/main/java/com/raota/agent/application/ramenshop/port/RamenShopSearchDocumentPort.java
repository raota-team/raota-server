package com.raota.agent.application.ramenshop.port;

import com.raota.agent.application.ramenshop.result.RamenShopSearchDocument;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface RamenShopSearchDocumentPort {

    List<RamenShopSearchDocument> searchShopDocuments(String query, int topK, double similarityThreshold);

    /**
     * {@code shopIds} 매장마다 매장 소개·리뷰 문서 중 {@code query}와 가장 비슷한 문서의 유사도다. 문서가 없는 매장은 빠진다.
     */
    Map<Long, Double> shopSimilarities(String query, Collection<Long> shopIds);

}
