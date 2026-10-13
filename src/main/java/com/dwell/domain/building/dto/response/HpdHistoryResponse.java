package com.dwell.domain.building.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(title = "HpdHistoryResponse DTO", description = "HPD History (OVERVIEW면 요약·차트, COMPLAINTS / VIOLATIONS면 목록, 해당 없는 필드는 null)")
public class HpdHistoryResponse {

    @Schema(description = "출처 문구", example = "NYC HPD public records")
    private String source;

    @Schema(description = "기간 (년)", example = "3")
    private int periodYears;

    @Schema(description = "민원 (OVERVIEW)")
    private OpenClosedCountResponse complaints;

    @Schema(description = "위반 (OVERVIEW)")
    private OpenClosedCountResponse violations;

    @Schema(description = "클래스별 위반 A/B/C (OVERVIEW)")
    private List<ViolationClassCountResponse> violationsByClass;

    @Schema(description = "분기별 건수 (최근 12분기, 오래된 순, OVERVIEW)")
    private List<QuarterCountResponse> perQuarter;

    @Schema(description = "HPD Online 링크", example = "https://hpdonline.nyc.gov/hpdonline/")
    private String hpdOnlineUrl;

    @Schema(description = "기록 목록 (COMPLAINTS / VIOLATIONS, 최신 순)")
    private List<HpdRecordResponse> content;

    @Schema(description = "페이지 번호 (목록 탭)", example = "0")
    private Integer page;

    @Schema(description = "페이지 크기 (목록 탭)", example = "20")
    private Integer size;

    @Schema(description = "전체 건수 (목록 탭)", example = "7")
    private Long totalElements;

    @Schema(description = "다음 페이지 여부 (목록 탭)", example = "false")
    private Boolean hasNext;
}
