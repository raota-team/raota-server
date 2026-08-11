package com.raota.agent.presentation.admin;

import com.raota.agent.application.retrieval.RetrievalIndexingService;
import com.raota.agent.application.retrieval.RetrievalIndexingService.ExternalReviewIndexResult;
import com.raota.agent.application.retrieval.RetrievalIndexingService.RetrievalDocumentResult;
import com.raota.agent.presentation.admin.request.ExternalReviewIndexRequest;
import com.raota.global.presentation.common.ApiResponse;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/api/retrieval")
@RequiredArgsConstructor
public class RetrievalAdminApiController {

    private final RetrievalIndexingService retrievalIndexingService;

    @PostMapping("/shops/reindex")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reindexAllShops() {
        retrievalIndexingService.indexAllShops();
        return ResponseEntity.ok(ApiResponse.success(
                "매장 프로필 색인이 완료되었습니다.",
                Map.of("scope", "all")
        ));
    }

    @PostMapping("/shops/{shopId}/reindex")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reindexShop(@PathVariable Long shopId) {
        retrievalIndexingService.indexShop(shopId);
        return ResponseEntity.ok(ApiResponse.success(
                "매장 프로필 색인이 완료되었습니다.",
                Map.of("scope", "single", "shopId", shopId)
        ));
    }

    @GetMapping("/shops/{shopId}/review-documents")
    public ResponseEntity<ApiResponse<List<RetrievalDocumentResult>>> getShopReviewDocuments(
            @PathVariable Long shopId,
            @RequestParam(defaultValue = "라멘 리뷰 맛 국물 면 메뉴 분위기") String query,
            @RequestParam(defaultValue = "12") int topK,
            @RequestParam(defaultValue = "0.2") double similarityThreshold
    ) {
        return ResponseEntity.ok(ApiResponse.success(retrievalIndexingService.searchShopReviewDocuments(
                shopId,
                query,
                topK,
                similarityThreshold
        )));
    }

    @PostMapping("/external-reviews/catchtable/reindex")
    public ResponseEntity<ApiResponse<ExternalReviewIndexResult>> reindexCatchtableReviews(
            @RequestBody ExternalReviewIndexRequest request
    ) {
        ExternalReviewIndexResult result = retrievalIndexingService.reindexCatchtableReviews(Path.of(request.path()));
        return ResponseEntity.ok(ApiResponse.success("캐치테이블 외부 리뷰 색인이 완료되었습니다.", result));
    }
}
