package com.dwell.domain.assistant.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(title = "CaseCandidateGroupResponse DTO", description = "상태별 케이스 그룹")
public class CaseCandidateGroupResponse {

    @Schema(description = "상태 배지 (LOGGED / IN_PROGRESS)", example = "LOGGED")
    private String statusBadge;

    @Schema(description = "케이스 (최신 생성 순)")
    private List<CaseCandidateResponse> cases;
}
