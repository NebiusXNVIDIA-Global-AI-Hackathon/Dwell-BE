package com.dwell.domain.cases.dto.request;

import com.dwell.domain.cases.enums.CaseEventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@NoArgsConstructor
@Setter
@Schema(title = "CaseEventCreateRequest DTO", description = "케이스 상태 업데이트(이벤트 기록) 요청")
public class CaseEventCreateRequest {

    @Schema(description = "이벤트 종류", example = "COMPLAINT_311_FILED")
    @NotNull(message = "Event type is a required field.")
    private CaseEventType type;

    @Schema(description = "발생 시각 (오프셋 포함 ISO 권장, 오프셋 없으면 뉴욕 시각. 생략하면 서버 현재 시각)", example = "2026-10-05T09:00:00-04:00")
    private String occurredAt;

    @Schema(description = "이벤트별 값 (예: srNumber, scheduledStart, visitedAt)", example = "{\"srNumber\": \"311-12345678\"}")
    private Map<String, Object> payload;

    @Schema(description = "메모 (선택)", example = "Filed online after no reply")
    @Size(max = 500, message = "Note must be 500 characters or less.")
    private String note;
}
