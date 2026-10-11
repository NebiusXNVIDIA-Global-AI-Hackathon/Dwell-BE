package com.dwell.domain.cases.dto.response;

import com.dwell.domain.cases.enums.StatusBadge;
import com.dwell.global.common.enums.Severity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(title = "CaseHeaderResponse: 케이스 화면 헤더 응답 DTO")
public class CaseHeaderResponse {

    @Schema(description = "케이스 ID", example = "6")
    private Long caseId;

    @Schema(description = "케이스 번호", example = "C-006")
    private String caseNumber;

    @Schema(description = "케이스 제목", example = "Water Leak in Bathroom · Ceiling")
    private String title;

    @Schema(description = "주소 (건물 + 호수)", example = "123 Main St, Apt 4B")
    private String address;

    @Schema(description = "상태 배지", example = "IN_PROGRESS")
    private StatusBadge statusBadge;

    @Schema(description = "심각도", example = "HIGH")
    private Severity severity;

    @Schema(description = "케이스 생성 시각", example = "2026-09-28T20:12:00")
    private LocalDateTime createdAt;
}
