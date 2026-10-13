package com.dwell.domain.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "TagSummaryResponse DTO", description = "태그 검색 요약 (#ceilingleak · 24 posts · 3 related to your case)")
public class TagSummaryResponse {

    @Schema(description = "검색한 태그 (#포함)", example = "#ceilingleak")
    private String tag;

    @Schema(description = "태그가 달린 글 수 (게시판·제목 필터 적용 후)", example = "24")
    private long postCount;

    @Schema(description = "그중 내 진행 중 케이스와 관련된 태그도 달린 글 수", example = "3")
    private long relatedToMyCase;
}
