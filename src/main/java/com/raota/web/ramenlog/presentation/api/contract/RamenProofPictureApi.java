package com.raota.web.ramenlog.presentation.api.contract;

import com.raota.global.presentation.common.ApiResponse;
import com.raota.global.presentation.common.PageResponse;
import com.raota.web.ramenlog.presentation.api.response.RamenShopProofPictureResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

@Tag(name = "인증샷", description = "라멘 인증샷 API")
public interface RamenProofPictureApi {

    @Operation(summary = "인증샷 목록 조회", description = "가게별 인증샷 목록을 페이징으로 조회합니다.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")})
    ResponseEntity<ApiResponse<PageResponse<RamenShopProofPictureResponse>>> getProofPicture(
            @Parameter(description = "가게 ID", required = true) Long shopId, @ParameterObject Pageable pageable);
}
