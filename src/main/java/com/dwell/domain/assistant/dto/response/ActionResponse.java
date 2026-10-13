package com.dwell.domain.assistant.dto.response;

import com.dwell.domain.assistant.enums.AssistantActionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@Schema(title = "ActionResponse DTO", description = "메시지에 붙은 선택 버튼")
public class ActionResponse {

    @Schema(description = "액션 ID (POST .../actions/{actionId})", example = "90")
    private Long actionId;

    @Schema(description = "액션 종류 (APPLY_LANDLORD_REPLY / SEND_MESSAGE / CREATE_CASE)", example = "SEND_MESSAGE")
    private AssistantActionType type;

    @Schema(description = "선택지 [{key, label}]")
    private List<Map<String, Object>> choices;

    @Schema(description = "이미 고른 선택지 key (null이면 아직 선택 전 -> 버튼 활성)", example = "YES")
    private String chosen;
}
