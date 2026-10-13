package com.dwell.domain.building.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "CategoryCountResponse DTO", description = "카테고리별 건수")
public class CategoryCountResponse {

    @Schema(description = "HPD 카테고리", example = "Water leak")
    private String category;

    @Schema(description = "건수", example = "6")
    private long count;
}
