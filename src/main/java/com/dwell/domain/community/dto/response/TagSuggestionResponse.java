package com.dwell.domain.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(title = "TagSuggestionResponse DTO", description = "태그 자동완성 (+ 검색창이면 제목 추천)")
public class TagSuggestionResponse {

    @Schema(description = "추천 태그 (입력과 같은 태그 먼저, 나머지는 글 많은 순, 최대 10개)")
    private List<TagCountResponse> items;

    @Schema(description = "입력과 정확히 같은 태그가 내 건물에 있는지 (false면 + Create new tag 노출)", example = "false")
    private boolean exactMatch;

    @Schema(description = "제목 추천 (withTitles=true일 때만, 최신 순 최대 5개, 아니면 null)")
    private List<TitleSuggestionResponse> titles;
}
