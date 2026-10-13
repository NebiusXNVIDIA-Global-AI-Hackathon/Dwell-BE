package com.dwell.domain.assistant.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
@Schema(title = "ActionChooseRequest DTO", description = "채팅 액션 선택 요청")
public class ActionChooseRequest {

    @NotBlank
    @Schema(description = "선택지 key (액션의 choices[].key, YES / NO)", example = "YES")
    private String choice;

    @Size(max = 2000)
    @Schema(description = "SEND_MESSAGE에서 Edit message로 고친 본문 (없으면 초안 그대로)", example = "Hi, thanks for confirming. Tuesday 10am works for me.")
    private String editedBody;
}
