package com.dwell.domain.assistant.dto.response;

import com.dwell.domain.assistant.enums.ConversationType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "ChatRoomCreateResponse DTO", description = "채팅방 생성 결과")
public class ChatRoomCreateResponse {

    @Schema(description = "대화 ID", example = "21")
    private Long conversationId;

    @Schema(description = "채팅방 유형", example = "CASE")
    private ConversationType type;

    @Schema(description = "연결된 케이스 (일반 채팅방이면 null)")
    @JsonProperty("case")
    private CaseSummaryResponse caseSummary;

    @Schema(description = "Deaver 첫 인사 메시지")
    private GreetingResponse greeting;

    // 새로 만들었으면 201, 기존 채팅방이면 200 (응답 바디에는 포함 안 됨)
    @JsonIgnore
    private boolean created;
}
