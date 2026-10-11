package com.dwell.domain.cases.dto.response;

import com.dwell.domain.evidence.enums.EvidencePhase;
import com.dwell.global.common.enums.MediaType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(title = "EvidenceItemResponse: 증거 썸네일 응답 DTO")
public class EvidenceItemResponse {

    @Schema(description = "증거 ID", example = "6")
    private Long evidenceId;

    @Schema(description = "미디어 타입", example = "MULTI")
    private MediaType mediaType;

    @Schema(description = "썸네일 (첫 번째 파일)", example = "evidence/6/f_8a1c.jpg")
    private String thumbnailUrl;

    @Schema(description = "묶인 파일 개수 (와프 3items)", example = "3")
    private int itemCount;

    @Schema(description = "증거 단계", example = "INITIAL")
    private EvidencePhase phase;

    @Schema(description = "등록 시각", example = "2026-09-28T20:12:00")
    private LocalDateTime createdAt;
}
