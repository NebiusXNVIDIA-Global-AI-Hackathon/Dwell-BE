package com.dwell.domain.assistant.dto.response;

import com.dwell.domain.cases.enums.CaseStage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "CaseUpdateResponse DTO", description = "케이스 stage 변경")
public class CaseUpdateResponse {

    @Schema(description = "케이스 ID", example = "5")
    private Long caseId;

    @Schema(description = "이전 stage", example = "AWAITING_RESPONSE")
    private CaseStage fromStage;

    @Schema(description = "바뀐 stage", example = "REPAIR_SCHEDULED")
    private CaseStage toStage;
}
