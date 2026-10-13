package com.dwell.domain.assistant.dto.response;

import com.dwell.global.common.enums.Location;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(title = "CaseCandidateResponse DTO", description = "채팅을 시작할 수 있는 케이스")
public class CaseCandidateResponse {

    @Schema(description = "케이스 ID", example = "5")
    private Long caseId;

    @Schema(description = "케이스 번호", example = "C-005")
    private String caseNumber;

    @Schema(description = "케이스 제목", example = "No heat")
    private String title;

    @Schema(description = "위치", example = "BATHROOM")
    private Location location;

    @Schema(description = "첫 번째 증거 사진 (10분 동안 유효한 presigned URL, 없으면 null)", example = "https://...")
    private String thumbnailUrl;

    @Schema(description = "생성 시각", example = "2026-09-28T14:00:00")
    private LocalDateTime createdAt;
}
