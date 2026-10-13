package com.dwell.domain.building.dto.response;

import com.dwell.global.common.enums.IssueCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "InspectionPrefillResponse DTO", description = "New Case 프리필")
public class InspectionPrefillResponse {

    @Schema(description = "이슈 카테고리", example = "PESTS")
    private IssueCategory issueCategory;

    @Schema(description = "세부 이슈 (아직 증상 -> 세부 이슈 매핑 없음, null)")
    private String issueDetail;

    @Schema(description = "패턴 문구", example = "2 reports in your building")
    private String patternText;
}
