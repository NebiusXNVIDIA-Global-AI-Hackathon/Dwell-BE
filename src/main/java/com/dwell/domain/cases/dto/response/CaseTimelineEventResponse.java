package com.dwell.domain.cases.dto.response;

import com.dwell.domain.cases.enums.CaseEventType;
import com.dwell.global.common.LocalizedText;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@Schema(title = "CaseTimelineEventResponse: 타임라인 이벤트 카드 응답 DTO")
public class CaseTimelineEventResponse {

    @Schema(description = "이벤트 ID", example = "1")
    private Long eventId;

    @Schema(description = "이벤트 타입", example = "CASE_CREATED")
    private CaseEventType type;

    @Schema(description = "발생 시각 (뉴욕 현지 시각)", example = "2026-09-28T20:12:00")
    private LocalDateTime occurredAt;

    @Schema(description = "카드 제목")
    private LocalizedText title;

    @Schema(description = "카드 설명 문장 (없으면 null)")
    private LocalizedText description;

    @Schema(description = "참조 대상 ID (통지·답장 등, 없으면 null)", example = "7")
    private Long refId;

    @Schema(description = "관련 증거 ID 목록 (없으면 빈 배열)", example = "[6, 7, 8]")
    private List<Long> evidenceIds;

    @Schema(description = "이벤트별 값 (일정·SR 번호·금액 등, 메모 제외, 없으면 null)")
    private Map<String, Object> meta;

    @Schema(description = "경고 표시 여부 (빨간 !)", example = "false")
    private boolean alert;
}
