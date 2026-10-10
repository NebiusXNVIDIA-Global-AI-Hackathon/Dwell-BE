package com.dwell.domain.assistant.dto.response;

import com.dwell.global.common.enums.Location;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "CaseSummaryResponse DTO", description = "채팅방에 연결된 케이스 요약")
public class CaseSummaryResponse {

    @Schema(description = "케이스 ID", example = "5")
    private Long caseId;

    @Schema(description = "케이스 번호", example = "C-005")
    private String caseNumber;

    @Schema(description = "케이스 제목", example = "No heat")
    private String title;

    @Schema(description = "위치", example = "LIVING_ROOM")
    private Location location;

    @Schema(description = "상태 배지 (LOGGED / IN_PROGRESS / RESOLVED)", example = "LOGGED")
    private String statusBadge;
}
