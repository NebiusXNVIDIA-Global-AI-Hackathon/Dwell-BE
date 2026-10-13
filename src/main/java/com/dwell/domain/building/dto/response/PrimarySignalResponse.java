package com.dwell.domain.building.dto.response;

import com.dwell.global.common.enums.IssueCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(title = "PrimarySignalResponse DTO", description = "메인 패턴 카드")
public class PrimarySignalResponse {

    @Schema(description = "시그널 ID", example = "12")
    private Long signalId;

    @Schema(description = "제목", example = "Water leak pattern detected")
    private String title;

    @Schema(description = "이슈 카테고리", example = "WATER_FLOODING")
    private IssueCategory issueCategory;

    @Schema(description = "이슈 이름", example = "Water leak")
    private String issueLabel;

    @Schema(description = "신고 수", example = "5")
    private int reportCount;

    @Schema(description = "신고된 위치 (시그널 breakdown.by_location, 없으면 [])", example = "[\"Bathroom\", \"Ceiling\", \"Kitchen\"]")
    private List<String> reportedIn;

    @Schema(description = "집계 기간", example = "2 months")
    private String windowLabel;

    @Schema(description = "내 진행 중 케이스와 같은 이슈인지", example = "true")
    private boolean relatedToMyCase;
}
