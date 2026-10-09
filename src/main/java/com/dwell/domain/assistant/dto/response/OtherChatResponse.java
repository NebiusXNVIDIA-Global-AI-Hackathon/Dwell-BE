package com.dwell.domain.assistant.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@Schema(title = "OtherChatResponse DTO", description = "일반 대화")
public class OtherChatResponse {

    @Schema(description = "채팅방 ID", example = "17")
    private Long conversationId;

    @Schema(description = "채팅 제목 (첫 사용자 메시지 앞 30자)", example = "My landlord isn't answering")
    private String title;

    @Schema(description = "마지막 메시지 (없으면 null)", example = "Deaver: Don't worry. First, send a written notice.")
    private String lastMessage;

    @Schema(description = "마지막 메시지 시각, UTC (없으면 null)", example = "2026-09-30T11:00:00Z")
    private Instant lastMessageAt;
}
