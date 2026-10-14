package com.dwell.domain.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(title = "MyPostListResponse DTO", description = "내 게시물 목록 (My Activities Posts 탭, 페이지)")
public class MyPostListResponse {

    @Schema(description = "탭 숫자 (게시물 수, 댓글 수)")
    private Counts counts;

    @Schema(description = "게시물 카드 목록")
    private List<MyPostResponse> content;

    @Schema(description = "페이지 번호 (0부터)", example = "0")
    private int page;

    @Schema(description = "페이지 크기", example = "20")
    private int size;

    @Schema(description = "전체 게시물 수", example = "1")
    private long totalElements;

    @Schema(description = "다음 페이지 여부", example = "false")
    private boolean hasNext;

    @Getter
    @Builder
    @Schema(title = "My Activities 탭 숫자")
    public static class Counts {

        @Schema(description = "내 게시물 수", example = "1")
        private long posts;

        @Schema(description = "내 댓글 수", example = "5")
        private long replies;
    }
}
