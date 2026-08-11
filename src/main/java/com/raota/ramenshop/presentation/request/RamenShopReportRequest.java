package com.raota.ramenshop.presentation.request;

import com.raota.ramenshop.domain.model.RamenShopReportType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RamenShopReportRequest {

    @Schema(description = "제보 유형", example = "OPENING_HOURS_ERROR")
    private RamenShopReportType reportType;

    @Schema(description = "상세 내용", example = "영업시간이 오후 9시까지로 변경되었습니다.")
    private String content;
}
