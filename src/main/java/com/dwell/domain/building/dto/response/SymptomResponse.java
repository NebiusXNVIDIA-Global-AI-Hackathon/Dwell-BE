package com.dwell.domain.building.dto.response;

import com.dwell.global.common.LocalizedText;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "SymptomResponse DTO", description = "증상 체크 항목")
public class SymptomResponse {

    @Schema(description = "증상 키 (결과 저장 시 symptoms로 보냄)", example = "ROACHES_SEEN")
    private String key;

    @Schema(description = "문구 (en / native)")
    private LocalizedText label;
}
