package com.dwell.domain.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "PostUpdateResponse DTO", description = "게시물 수정 결과")
public class PostUpdateResponse {

    @Schema(description = "게시물 ID", example = "40")
    private Long postId;
}
