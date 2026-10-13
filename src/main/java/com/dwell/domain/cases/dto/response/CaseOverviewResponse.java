package com.dwell.domain.cases.dto.response;

import com.dwell.domain.cases.enums.AffectedArea;
import com.dwell.domain.cases.enums.CaseStage;
import com.dwell.domain.cases.enums.CaseStatus;
import com.dwell.domain.cases.enums.StatusBadge;
import com.dwell.domain.cases.enums.Visibility;
import com.dwell.global.common.enums.AnalysisStatus;
import com.dwell.global.common.enums.IssueCategory;
import com.dwell.global.common.enums.Location;
import com.dwell.global.common.enums.Severity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Schema(title = "CaseOverviewResponse: 케이스 상세 응답 DTO")
public class CaseOverviewResponse {

    @Schema(description = "케이스 ID", example = "6")
    private Long caseId;

    @Schema(description = "케이스 번호", example = "C-006")
    private String caseNumber;

    @Schema(description = "케이스 제목", example = "Water Leak in Bathroom · Ceiling")
    private String title;

    @Schema(description = "주소 (건물 + 호수)", example = "123 Main St, Apt 4B")
    private String address;

    @Schema(description = "이슈 카테고리", example = "WATER_FLOODING")
    private IssueCategory issueCategory;

    @Schema(description = "이슈 상세", example = "WATER_LEAK")
    private String issueDetail;

    @Schema(description = "이슈 상세 직접 입력값 (issueDetail이 OTHER일 때 사용자가 입력한 값, 아니면 null)", example = "Strange smell from the drain", nullable = true)
    private String issueDetailOther;

    @Schema(description = "위치", example = "BATHROOM")
    private Location location;

    @Schema(description = "위치 직접 입력값 (location이 OTHER일 때 사용자가 입력한 값, 아니면 null)", example = "Basement storage room", nullable = true)
    private String locationOther;

    @Schema(description = "부위 (위치가 Entire Unit이면 null)", example = "CEILING", nullable = true)
    private AffectedArea affectedArea;

    @Schema(description = "부위 직접 입력값 (affectedArea가 OTHER일 때 사용자가 입력한 값, 아니면 null)", example = "Behind the mirror", nullable = true)
    private String affectedAreaOther;

    @Schema(description = "심각도", example = "HIGH")
    private Severity severity;

    @Schema(description = "공개 설정", example = "PUBLIC")
    private Visibility visibility;

    @Schema(description = "PRD 상태 (stage에서 파생)", example = "WAITING")
    private CaseStatus status;

    @Schema(description = "화면용 상태", example = "AWAITING_RESPONSE")
    private CaseStage stage;

    @Schema(description = "상태 배지", example = "IN_PROGRESS")
    private StatusBadge statusBadge;

    @Schema(description = "AI 분석 상태 (생성 직후 폴링용)", example = "DONE")
    private AnalysisStatus analysisStatus;

    @Schema(description = "케이스 생성 시각", example = "2026-09-28T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "체크리스트")
    private ChecklistResponse checklist;

    @Schema(description = "Current Status 배너")
    private CurrentStatusResponse currentStatus;

    @Schema(description = "진행 스텝 (최대 4개, 전체는 타임라인 API)")
    private List<TimelineStepResponse> timelineSteps;

    @Schema(description = "증거 요약")
    private CaseEvidenceResponse evidence;
}
