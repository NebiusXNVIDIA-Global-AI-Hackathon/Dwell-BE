package com.dwell.domain.building.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "MonthCountResponse DTO", description = "월별 건수")
public class MonthCountResponse {

    @Schema(description = "월 (yyyy-MM)", example = "2026-09")
    private String month;

    @Schema(description = "건수", example = "1")
    private long count;
}
