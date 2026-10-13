package com.dwell.domain.building.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "InspectionCreateResponse DTO", description = "Check My Unit 결과 저장")
public class InspectionCreateResponse {

    @Schema(description = "검사 결과 ID", example = "3")
    private Long inspectionId;

    @Schema(description = "체크한 증상 수", example = "2")
    private int matchedSigns;

    @Schema(description = "Create Case 프리필")
    private InspectionPrefillResponse prefill;
}
