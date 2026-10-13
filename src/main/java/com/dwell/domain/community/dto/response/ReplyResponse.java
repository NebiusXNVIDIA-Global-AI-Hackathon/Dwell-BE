package com.dwell.domain.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(title = "ReplyResponse DTO", description = "댓글")
public class ReplyResponse {

    @Schema(description = "댓글 ID", example = "77")
    private Long replyId;

    @Schema(description = "댓글 작성자")
    private ReplyAuthorResponse author;

    @Schema(description = "내용", example = "Same here. Ours started after the repair too.")
    private String content;

    @Schema(description = "작성 시각", example = "2026-10-04T08:00:00")
    private LocalDateTime createdAt;
}
