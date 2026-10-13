package com.dwell.domain.cases.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(title = "체크리스트 항목 추가 요청 DTO", description = "사용자가 직접 추가하는 체크리스트 항목")
public class ChecklistItemCreateRequest {

    @Schema(description = "항목 문구 (1~100자, 앞뒤 공백은 제거)", example = "Ask neighbor on 5F")
    @NotBlank(message = "Text is a required field.")
    @Size(max = 100, message = "Text must be 100 characters or less.")
    private String text;
}
