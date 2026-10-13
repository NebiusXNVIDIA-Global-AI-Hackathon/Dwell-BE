package com.dwell.domain.assistant.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@NoArgsConstructor
@Setter
@Schema(title = "MessageSendRequest DTO", description = "메시지 전송 요청 (quickReply를 누르면 그 문구를 content로 보냄)")
public class MessageSendRequest {

    @NotBlank
    @Size(max = 4000)
    @Schema(description = "메시지 (어떤 언어든 가능)", example = "집주인이 “We'll look into it”이라고만 하고 일주일째 연락이 없어요")
    private String content;

    @Size(max = 5)
    @Schema(description = "첨부 파일 ID (POST /files purpose=CHAT_ATTACHMENT, 최대 5개)", example = "[]")
    private List<Long> fileIds;
}
