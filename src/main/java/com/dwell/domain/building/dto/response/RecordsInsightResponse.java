package com.dwell.domain.building.dto.response;

import com.dwell.domain.building.enums.InsightStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(title = "RecordsInsightResponse DTO", description = "Building Insights · Records 탭")
public class RecordsInsightResponse {

    @Schema(description = "데이터 상태 (민원+위반 3건 미만이면 NOT_ENOUGH_DATA)", example = "OK")
    private InsightStatus status;

    @Schema(description = "집계 기간 (년)", example = "3")
    private int periodYears;

    @Schema(description = "민원")
    private OpenClosedCountResponse complaints;

    @Schema(description = "위반")
    private OpenClosedCountResponse violations;

    @Schema(description = "추세를 보여주는 최소 기록 수", example = "3")
    private int minimumRequired;

    @Schema(description = "카테고리별 민원 (많은 순, 데이터 부족이면 null)")
    private List<CategoryCountResponse> complaintsByCategory;

    @Schema(description = "최근 1년 3회 이상 반복된 민원 (가장 많은 것, 없으면 null)")
    private CategoryCountResponse repeatedIssue;

    @Schema(description = "월별 민원 (최근 36개월, 오래된 순, 데이터 부족이면 null)")
    private List<MonthCountResponse> complaintsPerMonth;

    @Schema(description = "출처 문구", example = "NYC HPD · updated daily")
    private String source;
}
