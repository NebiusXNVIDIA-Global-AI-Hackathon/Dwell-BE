package com.dwell.domain.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(title = "MyReplyListResponse DTO", description = "내 댓글 목록 (My Activities Replies 탭, 페이지)")
public class MyReplyListResponse {

    @Schema(description = "댓글 카드 목록")
    private List<MyReplyResponse> content;

    @Schema(description = "페이지 번호 (0부터)", example = "0")
    private int page;

    @Schema(description = "페이지 크기", example = "20")
    private int size;

    @Schema(description = "전체 댓글 수", example = "5")
    private long totalElements;

    @Schema(description = "다음 페이지 여부", example = "false")
    private boolean hasNext;
}
