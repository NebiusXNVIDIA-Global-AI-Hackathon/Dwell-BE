package com.dwell.domain.assistant.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(title = "CaseCandidatesResponse DTO", description = "Bring in a case 후보 목록")
public class CaseCandidatesResponse {

    @Schema(description = "상태별 그룹 (LOGGED -> IN_PROGRESS, 빈 그룹은 클라에서 숨김)")
    private List<CaseCandidateGroupResponse> groups;
}
