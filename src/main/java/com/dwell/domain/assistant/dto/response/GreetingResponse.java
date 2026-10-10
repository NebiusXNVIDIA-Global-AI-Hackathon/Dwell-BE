package com.dwell.domain.assistant.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(title = "GreetingResponse DTO", description = "Deaver 첫 인사 메시지")
public class GreetingResponse {

    @Schema(description = "메시지 ID", example = "301")
    private Long messageId;

    @Schema(description = "인사 내용", example = "I've loaded your No Heat case (C-005). What would you like to do first?")
    private String content;

    @Schema(description = "빠른 답장 버튼", example = "[\"Draft a notice to my landlord\", \"What are my rights?\"]")
    private List<String> quickReplies;
}
