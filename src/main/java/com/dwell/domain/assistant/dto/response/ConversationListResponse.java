package com.dwell.domain.assistant.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(title = "ConversationListResponse DTO", description = "채팅 목록")
public class ConversationListResponse {

    @Schema(description = "Case 채팅방 (해결된 Case 제외, 최근 메시지 순)")
    private List<CaseChatResponse> caseChats;

    @Schema(description = "Other 채팅방 (최근 메시지 순)")
    private List<OtherChatResponse> otherChats;
}
