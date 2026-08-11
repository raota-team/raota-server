package com.raota.agent.presentation.ramenshop;

import com.raota.account.infrastructure.auth.LoginMember;
import com.raota.agent.application.ramenshop.result.AiRamenShopSearchResult;
import com.raota.agent.application.ramenshop.result.RamenShopComparisonResult;
import com.raota.agent.application.ramenshop.service.AiRamenShopSearchService;
import com.raota.agent.application.ramenshop.service.RamenShopComparisonService;
import com.raota.agent.presentation.ramenshop.contract.AiRamenShopApi;
import com.raota.agent.presentation.ramenshop.request.AiRamenShopSearchRequest;
import com.raota.agent.presentation.ramenshop.request.RamenShopComparisonRequest;
import com.raota.agent.presentation.ramenshop.response.AiRamenShopSearchResponse;
import com.raota.agent.presentation.ramenshop.response.RamenShopComparisonResponse;
import com.raota.global.presentation.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ramen-shops")
@RequiredArgsConstructor
public class AiRamenShopController implements AiRamenShopApi {

    private final AiRamenShopSearchService searchService;
    private final RamenShopComparisonService comparisonService;

    @Override
    @PostMapping("/ai-search")
    public ResponseEntity<ApiResponse<AiRamenShopSearchResponse>> search(
            @RequestBody AiRamenShopSearchRequest request,
            @LoginMember(required = false) Long memberId) {
        String query = request == null ? null : request.query();
        return ResponseEntity.ok(ApiResponse.success(toResponse(searchService.search(query, memberId))));
    }

    @Override
    @PostMapping("/compare")
    public ResponseEntity<ApiResponse<RamenShopComparisonResponse>> compare(
            @RequestBody RamenShopComparisonRequest request) {
        Long shopAId = request == null ? null : request.shopAId();
        Long shopBId = request == null ? null : request.shopBId();
        String focus = request == null ? null : request.focus();
        return ResponseEntity.ok(ApiResponse.success(toResponse(
                comparisonService.compareShops(shopAId, shopBId, focus)
        )));
    }

    private AiRamenShopSearchResponse toResponse(AiRamenShopSearchResult result) {
        return new AiRamenShopSearchResponse(result.shops().stream()
                .map(shop -> new AiRamenShopSearchResponse.RecommendedShopResponse(
                        shop.id(), shop.name(), shop.type(), shop.location(), shop.description(),
                        shop.imageUrl(), shop.matchScore(), shop.bookmarked()
                ))
                .toList());
    }

    private RamenShopComparisonResponse toResponse(RamenShopComparisonResult result) {
        return new RamenShopComparisonResponse(
                new RamenShopComparisonResponse.ShopComparisonDetail(result.shopA().id(), result.shopA().name()),
                new RamenShopComparisonResponse.ShopComparisonDetail(result.shopB().id(), result.shopB().name()),
                result.focus(),
                result.narratives().stream()
                        .map(item -> new RamenShopComparisonResponse.ComparisonNarrative(item.title(), item.body()))
                        .toList()
        );
    }
}
