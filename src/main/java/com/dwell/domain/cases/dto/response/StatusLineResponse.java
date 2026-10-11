package com.dwell.domain.cases.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(title = "StatusLineResponse: 배너 날짜/값 라인 응답 DTO")
public class StatusLineResponse {

    @Schema(description = "라벨", example = "Complaint")
    private String label;

    @Schema(description = "값 (SR 번호, 위반 번호 등. 없으면 null)", example = "#311-12345678")
    private String value;

    @Schema(description = "시각", example = "2026-10-05T13:00:00")
    private LocalDateTime at;

    @Schema(description = "종료 시각 (수리·점검 시간대. 없으면 null)", example = "2026-10-08T16:00:00")
    private LocalDateTime endAt;
}
