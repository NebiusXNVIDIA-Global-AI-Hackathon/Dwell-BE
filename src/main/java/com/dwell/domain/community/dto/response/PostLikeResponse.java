package com.dwell.domain.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "PostLikeResponse DTO", description = "좋아요 / 좋아요 취소 결과")
public class PostLikeResponse {

    @Schema(description = "내 좋아요 여부", example = "true")
    private boolean liked;

    @Schema(description = "게시물 좋아요 수", example = "9")
    private long likeCount;
}
