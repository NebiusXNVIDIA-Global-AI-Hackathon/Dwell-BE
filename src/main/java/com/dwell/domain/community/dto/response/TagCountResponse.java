package com.dwell.domain.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "TagCountResponse DTO", description = "태그 + 내 건물 게시물 수")
public class TagCountResponse {

    @Schema(description = "태그 (#포함)", example = "#water")
    private String tag;

    @Schema(description = "이 태그가 달린 내 건물 게시물 수", example = "31")
    private long postCount;
}
