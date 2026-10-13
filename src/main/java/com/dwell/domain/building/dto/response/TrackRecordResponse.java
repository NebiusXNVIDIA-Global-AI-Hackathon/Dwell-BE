package com.dwell.domain.building.dto.response;

import com.dwell.domain.building.enums.LandlordRating;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "TrackRecordResponse DTO", description = "집주인 수리 기록 (HPD 위반 수리 소요일)")
public class TrackRecordResponse {

    @Schema(description = "배지", example = "SLOW_TO_FIX")
    private LandlordRating rating;

    @Schema(description = "이 건물 평균 수리일 (closed 위반 3건 미만이면 null)", example = "47")
    private Integer avgDaysToFix;

    @Schema(description = "NYC 평균 수리일", example = "30")
    private int nycAvgDaysToFix;

    @Schema(description = "게이지 최대값", example = "90")
    private int gaugeMaxDays;

    @Schema(description = "계산에 쓴 closed 위반 수", example = "5")
    private int fixedViolations;

    @Schema(description = "점수를 내는 최소 closed 위반 수", example = "3")
    private int minimumRequired;

    @Schema(description = "위반 (3년)")
    private OpenClosedCountResponse violations;

    @Schema(description = "Class C 위반 (3년)")
    private OpenClosedCountResponse classC;

    @Schema(description = "출처 문구", example = "NYC HPD violations, 3 yrs · computed")
    private String source;
}
