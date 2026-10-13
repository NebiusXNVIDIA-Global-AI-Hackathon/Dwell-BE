package com.dwell.domain.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(title = "PostListResponse DTO", description = "게시물 목록 (페이지)")
public class PostListResponse {

    @Schema(description = "태그 검색 요약 (tag로 검색했을 때만, 아니면 null)")
    private TagSummaryResponse tagSummary;

    @Schema(description = "게시물 카드 목록")
    private List<PostSummaryResponse> content;

    @Schema(description = "페이지 번호 (0부터)", example = "0")
    private int page;

    @Schema(description = "페이지 크기", example = "20")
    private int size;

    @Schema(description = "전체 게시물 수", example = "4")
    private long totalElements;

    @Schema(description = "다음 페이지 여부", example = "false")
    private boolean hasNext;
}
