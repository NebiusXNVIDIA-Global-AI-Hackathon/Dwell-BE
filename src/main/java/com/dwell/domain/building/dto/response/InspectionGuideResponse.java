package com.dwell.domain.building.dto.response;

import com.dwell.global.common.enums.IssueCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(title = "InspectionGuideResponse DTO", description = "Check My Unit 체크리스트")
public class InspectionGuideResponse {

    @Schema(description = "시그널 ID", example = "14")
    private Long signalId;

    @Schema(description = "이슈 카테고리", example = "PESTS")
    private IssueCategory issueCategory;

    @Schema(description = "부제", example = "Pests · based on 2 neighbor reports")
    private String subtitle;

    @Schema(description = "증상 체크리스트")
    private List<SymptomResponse> symptoms;
}
