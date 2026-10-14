package com.dwell.domain.home.dto.response;

import com.dwell.domain.cases.dto.response.TimelineStepResponse;
import com.dwell.domain.cases.enums.CaseStage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Schema(title = "HomeCaseResponse DTO", description = "홈 케이스 카드 (캐러셀 한 장)")
public class HomeCaseResponse {

    @Schema(description = "케이스 ID (View Case → GET /cases/{caseId})", example = "6")
    private Long caseId;

    @Schema(description = "케이스 번호", example = "C-006")
    private String caseNumber;

    @Schema(description = "세부 이슈 이름 (Other면 직접 입력한 값)", example = "Water Leak")
    private String title;

    @Schema(description = "공간 - 부위 (Entire Unit이면 공간만)", example = "Bathroom - Ceiling")
    private String locationLabel;

    @Schema(description = "진행 단계", example = "AWAITING_RESPONSE")
    private CaseStage stage;

    @Schema(description = "마지막 업데이트 (Last update)", example = "2026-09-29T10:42:00")
    private LocalDateTime updatedAt;

    @Schema(description = "미니 타임라인 (케이스 상세와 같은 데이터)")
    private List<TimelineStepResponse> timelineSteps;

    @Schema(description = "Checklist for Next Step")
    private List<HomeChecklistItemResponse> checklist;
}
