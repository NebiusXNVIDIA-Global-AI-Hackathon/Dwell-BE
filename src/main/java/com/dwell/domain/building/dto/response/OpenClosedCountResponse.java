package com.dwell.domain.building.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "OpenClosedCountResponse DTO", description = "건수 (전체 / open / closed)")
public class OpenClosedCountResponse {

    @Schema(description = "전체", example = "7")
    private long total;

    @Schema(description = "open", example = "2")
    private long open;

    @Schema(description = "closed", example = "5")
    private long closed;
}
