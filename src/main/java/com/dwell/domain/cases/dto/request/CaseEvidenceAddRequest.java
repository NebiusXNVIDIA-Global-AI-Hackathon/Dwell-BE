package com.dwell.domain.cases.dto.request;

import com.dwell.domain.evidence.enums.EvidencePhase;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Schema(title = "케이스 증거 추가 요청 DTO", description = "Case Detail에서 올린 증거 파일과 단계, 메모")
public class CaseEvidenceAddRequest {

    @Schema(description = "POST /files로 올린 파일 ID (1개 이상)", example = "[110, 111]")
    private List<Long> fileIds;

    @Schema(description = "증거 단계 (생략하면 INITIAL, AFTER_REPAIR면 수리 검증 시작)", example = "PROGRESS")
    private EvidencePhase phase;

    @Schema(description = "증거 메모 (선택, Add a description 화면)", example = "Ceiling patched but still damp")
    @Size(max = 500, message = "Note must be 500 characters or less.")
    private String note;
}
