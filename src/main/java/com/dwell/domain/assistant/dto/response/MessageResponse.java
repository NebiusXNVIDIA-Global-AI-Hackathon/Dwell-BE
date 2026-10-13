package com.dwell.domain.assistant.dto.response;

import com.dwell.domain.assistant.enums.MessageRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@Schema(title = "MessageResponse DTO", description = "채팅 메시지")
public class MessageResponse {

    @Schema(description = "메시지 ID", example = "303")
    private Long messageId;

    @Schema(description = "보낸 쪽 (USER / ASSISTANT)", example = "ASSISTANT")
    private MessageRole role;

    @Schema(description = "본문", example = "During heat season (Oct 1 – May 31), your landlord must keep your apartment warm.")
    private String content;

    @Schema(description = "카드 (TRANSLATION / LEGAL_RULE / CASE_PREFILL / MESSAGE_DRAFT, 없으면 [])")
    private List<Map<String, Object>> cards;

    @Schema(description = "법률 근거 [{label, url}] (없으면 [])")
    private List<Map<String, Object>> citations;

    @Schema(description = "빠른 답장 버튼 (없으면 [])", example = "[\"Draft a notice to my landlord\"]")
    private List<String> quickReplies;

    @Schema(description = "선택 버튼 (없으면 null)")
    private ActionResponse action;

    @Schema(description = "첨부 파일 (사용자 메시지, 없으면 [])")
    private List<AttachmentResponse> attachments;

    @Schema(description = "보낸 시각", example = "2026-10-04T13:13:05")
    private LocalDateTime createdAt;
}
