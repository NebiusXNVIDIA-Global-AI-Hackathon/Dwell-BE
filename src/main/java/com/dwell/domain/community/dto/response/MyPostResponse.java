package com.dwell.domain.community.dto.response;

import com.dwell.domain.community.enums.Board;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Schema(title = "MyPostResponse DTO", description = "내 게시물 카드 (My Activities Posts 탭)")
public class MyPostResponse {

    @Schema(description = "게시물 ID", example = "40")
    private Long postId;

    @Schema(description = "제목", example = "Pipe burst in my unit")
    private String title;

    @Schema(description = "게시판", example = "FREE")
    private Board board;

    @Schema(description = "태그", example = "[\"#ceilingleak\", \"#floor5\"]")
    private List<String> tags;

    @Schema(description = "좋아요 수", example = "9")
    private long likeCount;

    @Schema(description = "댓글 수", example = "2")
    private long replyCount;

    @Schema(description = "작성 시각", example = "2026-10-04T10:00:00")
    private LocalDateTime createdAt;
}
