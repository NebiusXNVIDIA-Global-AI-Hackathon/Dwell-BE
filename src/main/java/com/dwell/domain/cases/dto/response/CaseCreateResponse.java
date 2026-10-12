package com.dwell.domain.cases.dto.response;

import com.dwell.domain.cases.enums.CaseStage;
import com.dwell.global.common.enums.AnalysisStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "케이스 생성 응답 DTO", description = "생성 직후 상태 (AI 분석 결과는 GET /cases/{caseId} 폴링으로 확인)")
public class CaseCreateResponse {

    @Schema(description = "케이스 ID", example = "6")
    private Long caseId;

    @Schema(description = "케이스 번호", example = "C-006")
    private String caseNumber;

    @Schema(description = "화면용 상태 (생성 직후 LOGGED)", example = "LOGGED")
    private CaseStage stage;

    @Schema(description = "AI 분석 상태 (생성 직후 PENDING)", example = "PENDING")
    private AnalysisStatus analysisStatus;
}
