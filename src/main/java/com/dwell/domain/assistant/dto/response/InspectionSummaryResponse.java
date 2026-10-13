package com.dwell.domain.assistant.dto.response;

import com.dwell.global.common.enums.IssueCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "InspectionSummaryResponse DTO", description = "채팅방에 연결된 내 집 확인(Check My Unit) 결과 요약")
public class InspectionSummaryResponse {

    @Schema(description = "검사 결과 ID", example = "3")
    private Long inspectionId;

    @Schema(description = "건물 시그널 ID (시그널이 지워졌으면 null)", example = "14")
    private Long signalId;

    @Schema(description = "이슈 분류", example = "PESTS")
    private IssueCategory issueCategory;
}
