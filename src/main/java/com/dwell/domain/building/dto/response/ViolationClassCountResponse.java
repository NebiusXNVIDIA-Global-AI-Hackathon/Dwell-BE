package com.dwell.domain.building.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "ViolationClassCountResponse DTO", description = "클래스별 위반 수")
public class ViolationClassCountResponse {

    @Schema(description = "위반 클래스 (A / B / C)", example = "C")
    private String violationClass;

    @Schema(description = "전체", example = "1")
    private long total;

    @Schema(description = "open", example = "1")
    private long open;
}
