package com.dwell.domain.home.dto.response;

import com.dwell.global.common.LocalizedText;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "HomeChecklistItemResponse DTO", description = "Checklist for Next Step 항목")
public class HomeChecklistItemResponse {

    @Schema(description = "체크리스트 항목 ID (체크는 PATCH /cases/{caseId}/checklist-items/{itemId})", example = "31")
    private Long itemId;

    @Schema(description = "항목 문구 (native는 모국어 번역, 아직 없으면 null)")
    private LocalizedText text;

    @Schema(description = "체크 여부", example = "false")
    private boolean checked;
}
