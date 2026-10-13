package com.dwell.domain.building.dto.response;

import com.dwell.global.common.enums.IssueCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "SignalItemResponse DTO", description = "Rising Issue 항목")
public class SignalItemResponse {

    @Schema(description = "시그널 ID", example = "14")
    private Long signalId;

    @Schema(description = "이슈 카테고리", example = "PESTS")
    private IssueCategory issueCategory;

    @Schema(description = "제목", example = "Pests")
    private String title;

    @Schema(description = "설명", example = "A few neighbors reported pest activity.")
    private String description;

    @Schema(description = "신고 수 (앱 공개 케이스 + HPD 기록)", example = "2")
    private int reportCount;
}
