package com.dwell.domain.cases.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(title = "CaseTimelineResponse: 케이스 타임라인 상세 응답 DTO")
public class CaseTimelineResponse {

    @Schema(description = "화면 헤더 (케이스 번호·제목·주소·배지·생성일)")
    private CaseHeaderResponse header;

    @Schema(description = "타임라인 이벤트 (오래된 순)")
    private List<CaseTimelineEventResponse> events;
}
