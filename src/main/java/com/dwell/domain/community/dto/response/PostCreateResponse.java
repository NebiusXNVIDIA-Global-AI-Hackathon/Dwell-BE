package com.dwell.domain.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "PostCreateResponse DTO", description = "게시물 작성 결과")
public class PostCreateResponse {

    @Schema(description = "게시물 ID", example = "40")
    private Long postId;
}
