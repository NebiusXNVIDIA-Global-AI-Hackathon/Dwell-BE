package com.dwell.domain.assistant.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
@Schema(title = "ActionResultResponse DTO", description = "채팅 액션 실행 결과")
public class ActionResultResponse {

    @Schema(description = "액션 ID", example = "90")
    private Long actionId;

    @Schema(description = "고른 선택지 key", example = "YES")
    private String chosen;

    @Schema(description = "CREATE_CASE + YES: New Case 프리필 (issueCategory, issueDetail, location, affectedArea, description)")
    private Map<String, Object> prefill;

    @Schema(description = "SEND_MESSAGE + YES: 보낼 최종 본문 (클라가 sms: / mailto:로 열기)", example = "Hi, thanks for confirming the repair on Oct 6.")
    private String messageBody;

    @Schema(description = "APPLY_LANDLORD_REPLY + YES: 답장 초안 메시지 (MESSAGE_DRAFT 카드 + SEND_MESSAGE 액션)")
    private MessageResponse nextMessage;

    @Schema(description = "케이스 stage가 바뀌었으면 {caseId, fromStage, toStage}, 아니면 null")
    private CaseUpdateResponse caseUpdate;
}
