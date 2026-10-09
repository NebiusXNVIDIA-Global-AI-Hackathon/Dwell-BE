package com.dwell.domain.assistant.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@Schema(title = "CaseChatResponse DTO", description = "케이스 대화")
public class CaseChatResponse {

    @Schema(description = "채팅방 ID", example = "21")
    private Long conversationId;

    @Schema(description = "케이스 ID", example = "6")
    private Long caseId;

    @Schema(description = "채팅 제목 (이슈 유형 · 케이스 번호)", example = "Water Leak · C-006")
    private String title;

    @Schema(description = "마지막 메시지 (없으면 null)", example = "Deaver: If anything else needs updating, just let me know.")
    private String lastMessage;

    @Schema(description = "마지막 메시지 시각, UTC (없으면 null)", example = "2026-10-07T09:58:00Z")
    private Instant lastMessageAt;
}
