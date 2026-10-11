package com.dwell.domain.cases.dto.response;

import com.dwell.domain.cases.enums.BannerTone;
import com.dwell.global.common.LocalizedText;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Schema(title = "CurrentStatusResponse: Current Status 배너 응답 DTO")
public class CurrentStatusResponse {

    @Schema(description = "배너 제목")
    private LocalizedText title;

    @Schema(description = "배너 색", example = "DEFAULT")
    private BannerTone tone;

    // D-3 → -3, D-Day → 0, D+1 → 1 (클라가 제목 앞에 붙임)
    @Schema(description = "D-day (없으면 null)", example = "-3")
    private Integer dDay;

    @Schema(description = "날짜/값 라인")
    private List<StatusLineResponse> lines;

    @Schema(description = "진행바 시작 시각 (진행바 없으면 null)", example = "2026-09-28T15:00:00")
    private LocalDateTime startAt;

    @Schema(description = "응답 기한 (남은 시간·초과 시간 계산용)", example = "2026-09-29T15:00:00")
    private LocalDateTime dueAt;

    @Schema(description = "배너 버튼")
    private StatusCtaResponse primaryCta;

    // Lombok getDDay()는 JSON 키가 "DDay"로 나가서 직접 지정
    @JsonProperty("dDay")
    public Integer getDDay() {
        return dDay;
    }
}
