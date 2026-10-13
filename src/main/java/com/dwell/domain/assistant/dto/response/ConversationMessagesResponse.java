package com.dwell.domain.assistant.dto.response;

import com.dwell.domain.assistant.enums.ConversationType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(title = "ConversationMessagesResponse DTO", description = "대화 조회 (메시지 페이지)")
public class ConversationMessagesResponse {

    @Schema(description = "채팅방 ID", example = "21")
    private Long conversationId;

    @Schema(description = "채팅방 유형 (OTHER / CASE / INSPECTION)", example = "CASE")
    private ConversationType type;

    @Schema(description = "헤더 제목 (CASE: 케이스 제목, INSPECTION: Possible {이슈} Issue, OTHER: 첫 사용자 메시지 앞 30자 또는 New Chat)", example = "No heat")
    private String title;

    @Schema(description = "연결된 케이스 (일반 채팅방이면 null)")
    @JsonProperty("case")
    private CaseSummaryResponse caseSummary;

    @Schema(description = "연결된 내 집 확인 결과 (INSPECTION 채팅방이 아니면 null)")
    private InspectionSummaryResponse inspection;

    @Schema(description = "메시지 (오래된 순)")
    private List<MessageResponse> messages;

    @Schema(description = "더 이전 메시지가 있는지 (있으면 before=첫 messageId로 다시 조회)", example = "false")
    private boolean hasMore;
}
