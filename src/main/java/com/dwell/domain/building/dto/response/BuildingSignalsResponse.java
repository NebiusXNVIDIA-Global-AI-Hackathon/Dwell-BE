package com.dwell.domain.building.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(title = "BuildingSignalsResponse DTO", description = "Building Signals")
public class BuildingSignalsResponse {

    @Schema(description = "내 진행 중 케이스와 같은 이슈 중 가장 강한 패턴 (없으면 null)")
    private PrimarySignalResponse primary;

    @Schema(description = "나머지 시그널 (신고 많은 순)")
    private List<SignalItemResponse> risingIssues;
}
