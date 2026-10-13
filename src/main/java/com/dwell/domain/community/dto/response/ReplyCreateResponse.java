package com.dwell.domain.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "ReplyCreateResponse DTO", description = "댓글 작성 결과")
public class ReplyCreateResponse {

    @Schema(description = "댓글 ID", example = "78")
    private Long replyId;
}
