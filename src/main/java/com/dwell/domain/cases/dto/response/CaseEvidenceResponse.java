package com.dwell.domain.cases.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(title = "CaseEvidenceResponse: 케이스 증거 요약 응답 DTO")
public class CaseEvidenceResponse {

    @Schema(description = "증거 개수", example = "1")
    private int count;

    @Schema(description = "증거 목록 (등록순)")
    private List<EvidenceItemResponse> items;
}
