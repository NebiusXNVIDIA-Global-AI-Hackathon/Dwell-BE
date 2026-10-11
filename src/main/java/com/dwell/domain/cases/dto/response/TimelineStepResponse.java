package com.dwell.domain.cases.dto.response;

import com.dwell.domain.cases.enums.TimelineStepState;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@Schema(title = "TimelineStepResponse: 케이스 진행 스텝 응답 DTO")
public class TimelineStepResponse {

    @Schema(description = "스텝 라벨", example = "Notified Landlord")
    private String label;

    @Schema(description = "날짜 (없으면 null)", example = "2026-09-28")
    private LocalDate date;

    @Schema(description = "스텝 상태", example = "DONE")
    private TimelineStepState state;
}
