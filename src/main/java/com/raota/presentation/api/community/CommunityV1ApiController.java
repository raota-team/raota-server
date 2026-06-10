package com.raota.presentation.api.community;

import com.raota.domain.community.repository.query.PostQueryRepository;
import com.raota.presentation.api.community.response.CommunityHomePostResponse;
import com.raota.presentation.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "홈 화면 - 커뮤니티", description = "홈 화면용 커뮤니티 API")
@RestController
@RequestMapping("/api/v1/community")
@RequiredArgsConstructor
public class CommunityV1ApiController {

    private final PostQueryRepository postQueryRepository;

    @Operation(summary = "라멘 꿀팁 조회", description = "커뮤니티의 '꿀팁' 카테고리 게시글 목록을 요약 형태로 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<List<CommunityHomePostResponse>>> getHomePosts(
            @Parameter(description = "카테고리 (기본: tip)")
            @RequestParam(defaultValue = "tip") String category,
            @Parameter(description = "가져올 개수", example = "3")
            @RequestParam(defaultValue = "3") int limit) {
        // Note: Sort is latest by default in findHomePosts
        return ResponseEntity.ok(ApiResponse.success(postQueryRepository.findHomePosts(category, limit)));
    }
}
