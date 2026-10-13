package com.dwell.domain.community.dto.response;

import com.dwell.domain.community.enums.Board;
import com.dwell.global.common.enums.FloorBand;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "TitleSuggestionResponse DTO", description = "검색창 제목 추천 (Ceiling leak in bathroom — what I did · Tips · lower floor)")
public class TitleSuggestionResponse {

    @Schema(description = "게시물 ID", example = "35")
    private Long postId;

    @Schema(description = "제목", example = "Ceiling leak in bathroom — what I did")
    private String title;

    @Schema(description = "게시판 (FREE / TIPS)", example = "TIPS")
    private Board board;

    @Schema(description = "작성자 층 구간 (LOWER / MIDDLE / UPPER)", example = "LOWER")
    private FloorBand floorBand;
}
