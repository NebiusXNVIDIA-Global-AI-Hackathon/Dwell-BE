package com.dwell.domain.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "PostImageResponse DTO", description = "게시물 이미지")
public class PostImageResponse {

    @Schema(description = "파일 ID (수정 시 imageFileIds로 사용)", example = "130")
    private Long fileId;

    @Schema(description = "이미지 URL (10분 동안 유효한 presigned URL)", example = "https://...")
    private String url;
}
