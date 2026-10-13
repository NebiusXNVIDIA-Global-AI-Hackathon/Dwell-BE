package com.dwell.domain.building.dto.response;

import com.dwell.domain.building.enums.HpdRecordType;
import com.dwell.domain.building.enums.HpdStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@Schema(title = "HpdRecordResponse DTO", description = "HPD 기록")
public class HpdRecordResponse {

    @Schema(description = "접수일", example = "2026-09-12")
    private LocalDate date;

    @Schema(description = "종류 (COMPLAINT / VIOLATION)", example = "VIOLATION")
    private HpdRecordType kind;

    @Schema(description = "위반 클래스 (위반만, 민원은 null)", example = "C")
    private String violationClass;

    @Schema(description = "HPD 카테고리", example = "Failure to maintain building")
    private String category;

    @Schema(description = "상태 (OPEN / CLOSED)", example = "OPEN")
    private HpdStatus status;
}
