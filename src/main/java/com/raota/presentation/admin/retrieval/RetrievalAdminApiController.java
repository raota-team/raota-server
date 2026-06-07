package com.raota.presentation.admin.retrieval;

import com.raota.application.retrieval.RetrievalIndexingService;
import com.raota.application.retrieval.RetrievalIndexingService.ExternalReviewIndexResult;
import com.raota.presentation.admin.retrieval.request.ExternalReviewIndexRequest;
import com.raota.presentation.common.ApiResponse;
import java.nio.file.Path;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @PostMapping("/external-reviews/catchtable/reindex")
    public ResponseEntity<ApiResponse<ExternalReviewIndexResult>> reindexCatchtableReviews(
            @RequestBody ExternalReviewIndexRequest request
    ) {
        ExternalReviewIndexResult result = retrievalIndexingService.reindexCatchtableReviews(Path.of(request.path()));
        return ResponseEntity.ok(ApiResponse.success("캐치테이블 외부 리뷰 색인이 완료되었습니다.", result));
    }
}
