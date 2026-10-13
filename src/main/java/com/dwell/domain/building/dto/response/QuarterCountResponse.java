package com.dwell.domain.building.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "QuarterCountResponse DTO", description = "분기별 건수")
public class QuarterCountResponse {

    @Schema(description = "분기", example = "2026-Q3")
    private String quarter;

    @Schema(description = "민원", example = "2")
    private long complaints;

    @Schema(description = "위반", example = "1")
    private long violations;
}
