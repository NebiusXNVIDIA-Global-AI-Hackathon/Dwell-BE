package com.dwell.domain.community.dto.response;

import com.dwell.domain.building.enums.InsightStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@Schema(title = "LandlordPostSummaryResponse DTO", description = "Building Insights · Tenants 탭 (집주인 언급 글 요약)")
public class LandlordPostSummaryResponse {

    @Schema(description = "데이터 상태 (집주인 언급 글 3개 미만이면 NOT_ENOUGH_DATA)", example = "OK")
    private InsightStatus status;

    @Schema(description = "집주인 언급 글 수", example = "8")
    private long postCount;

    @Schema(description = "요약을 보여주는 최소 글 수", example = "3")
    private int minimumRequired;

    @Schema(description = "분류별 글 수 (SLOW_REPAIRS / NO_REPLY / FIXED_WELL, 데이터 부족이면 null)")
    private Map<String, Long> topicCounts;

    @Schema(description = "미리보기 글 (좋아요 많은 순 3개, 데이터 부족이면 null)")
    private List<LandlordPostResponse> previewPosts;
}
