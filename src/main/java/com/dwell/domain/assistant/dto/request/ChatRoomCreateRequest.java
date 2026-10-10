package com.dwell.domain.assistant.dto.request;

import com.dwell.domain.assistant.enums.ConversationIntent;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
@Schema(title = "ChatRoomCreateRequest DTO", description = "채팅방 생성 요청")
public class ChatRoomCreateRequest {

    @Schema(description = "케이스 ID (없으면 일반 채팅방)", example = "5")
    private Long caseId;

    @Schema(description = "진입 목적 (선택, Update status에서 진입 시 REPLY_RECEIVED)", example = "REPLY_RECEIVED")
    private ConversationIntent intent;
}
