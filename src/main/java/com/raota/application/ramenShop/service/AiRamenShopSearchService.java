package com.raota.application.ramenShop.service;

import com.raota.application.member.BookmarkService;
import com.raota.application.ramenShop.port.FileUrlPort;
import com.raota.application.ramenShop.port.RamenShopSearchDocumentPort;
import com.raota.application.ramenShop.query.ParsedAiRamenShopSearchQuery;
import com.raota.application.ramenShop.result.AiRamenShopSearchHit;
import com.raota.application.ramenShop.result.AiRamenShopSearchResult;
import com.raota.application.ramenShop.search.AiRamenShopSearchQueryParser;
import com.raota.application.ramenShop.search.AiRamenShopSearchReranker;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AiRamenShopSearchService {

    private final RamenShopSearchDocumentPort searchDocumentPort;
    private final RamenShopRepository ramenShopRepository;
    private final FileUrlPort fileUrlPort;
    private final BookmarkService bookmarkService;
    private final AiRamenShopSearchQueryParser queryParser;
    private final AiRamenShopSearchReranker reranker;


    public AiRamenShopSearchService(
            RamenShopSearchDocumentPort searchDocumentPort,
            RamenShopRepository ramenShopRepository,
            FileUrlPort fileUrlPort,
            BookmarkService bookmarkService,
            AiRamenShopSearchQueryParser queryParser,
            AiRamenShopSearchReranker reranker
    ) {
        this.bookmarkService = bookmarkService;
        this.searchDocumentPort = searchDocumentPort;
        this.ramenShopRepository = ramenShopRepository;
        this.fileUrlPort = fileUrlPort;
        this.queryParser = queryParser;
        this.reranker = reranker;
    }

    public AiRamenShopSearchResult search(String query, Long memberId) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("검색어는 필수입니다.");
        }
        ParsedAiRamenShopSearchQuery parsedQuery = queryParser.parse(query.trim());
        List<AiRamenShopSearchHit> searchResult = searchRelevantShopDocuments(parsedQuery);

        return buildSearchResponse(searchResult, memberId);
    }

    private List<AiRamenShopSearchHit> searchRelevantShopDocuments(ParsedAiRamenShopSearchQuery query) {
        return reranker.rerank(searchDocumentPort.searchShopDocuments(query.expandedQuery(), 30, 0.35), query, 6);
    }

    private AiRamenShopSearchResult buildSearchResponse(
            List<AiRamenShopSearchHit> searchResult,
            Long memberId) {
        List<AiRamenShopSearchResult.ShopResult> recommendedShops = searchResult.stream()
                .map(hit -> toRecommendedShopResponse(hit, memberId))
                .toList();

        return new AiRamenShopSearchResult(recommendedShops);
    }

    private AiRamenShopSearchResult.ShopResult toRecommendedShopResponse(
            AiRamenShopSearchHit hit,
            Long memberId) {
        RamenShop shop = ramenShopRepository.findById(hit.shopId()).orElseThrow();

        return new AiRamenShopSearchResult.ShopResult(
                shop.getId(),
                shop.getName(),
                primaryTag(shop),
                shop.getAddress() == null ? "" : shop.getAddress().fullAddress(),
                shop.getDescription(),
                fileUrlPort.getAccessibleUrl(shop.getImageUrl()),
                Math.min(100, (int) Math.round(hit.finalScore() * 100)),
                bookmarkService.isBookmarked(memberId, shop.getId())
        );
    }

    private String primaryTag(RamenShop shop) {
        if (shop.getTags() == null || shop.getTags().isEmpty()) {
            return "";
        }
        return shop.getTags().getFirst();
    }
}
