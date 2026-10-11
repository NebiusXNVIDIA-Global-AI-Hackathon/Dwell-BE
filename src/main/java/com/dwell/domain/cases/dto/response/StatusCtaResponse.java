package com.dwell.domain.cases.dto.response;

import com.dwell.domain.cases.enums.BannerCtaType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "StatusCtaResponse: 배너 버튼 응답 DTO")
public class StatusCtaResponse {

    @Schema(description = "버튼 동작", example = "UPDATE_STATUS")
    private BannerCtaType type;

    @Schema(description = "버튼 문구", example = "Got a Reply? Update Status")
    private String label;
}
