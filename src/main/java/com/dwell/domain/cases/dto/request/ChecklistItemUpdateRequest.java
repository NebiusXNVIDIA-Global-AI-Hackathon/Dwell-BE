package com.dwell.domain.cases.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(title = "체크리스트 항목 수정 요청 DTO", description = "체크 여부와 문구 중 보낸 값만 수정 (둘 다 생략하면 400)")
public class ChecklistItemUpdateRequest {

    @Schema(description = "체크 여부 (선택)", example = "true")
    private Boolean checked;

    @Schema(description = "항목 문구 (선택, 1~100자, 앞뒤 공백은 제거). AI 항목도 수정 가능하며 수정하면 번역본은 비워짐", example = "Ask neighbor on 5F")
    @Size(max = 100, message = "Text must be 100 characters or less.")
    private String text;
}
