package com.dwell.domain.cases.dto.response;

import com.dwell.domain.cases.enums.ChecklistSource;
import com.dwell.global.common.LocalizedText;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "ChecklistItemResponse: 체크리스트 항목 응답 DTO")
public class ChecklistItemResponse {

    @Schema(description = "항목 ID", example = "31")
    private Long itemId;

    @Schema(description = "항목 문구 (USER 항목은 native가 null)")
    private LocalizedText text;

    @Schema(description = "체크 여부", example = "false")
    private boolean checked;

    @Schema(description = "생성 주체", example = "AI")
    private ChecklistSource source;
}
