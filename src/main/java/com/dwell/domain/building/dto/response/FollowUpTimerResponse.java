package com.dwell.domain.building.dto.response;

import com.dwell.domain.building.enums.FollowUpReason;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "FollowUpTimerResponse DTO", description = "Follow-up timer")
public class FollowUpTimerResponse {

    @Schema(description = "기본 시간", example = "72")
    private int defaultHours;

    @Schema(description = "이 집주인에게 적용되는 시간", example = "48")
    private int appliedHours;

    @Schema(description = "사유", example = "SHORTENED")
    private FollowUpReason reason;
}
