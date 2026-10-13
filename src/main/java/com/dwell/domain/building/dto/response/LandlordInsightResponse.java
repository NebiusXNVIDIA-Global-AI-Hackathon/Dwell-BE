package com.dwell.domain.building.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "LandlordInsightResponse DTO", description = "Building Insights · Landlord 탭")
public class LandlordInsightResponse {

    @Schema(description = "집주인 수리 기록")
    private TrackRecordResponse trackRecord;

    @Schema(description = "Follow-up timer")
    private FollowUpTimerResponse followUpTimer;
}
