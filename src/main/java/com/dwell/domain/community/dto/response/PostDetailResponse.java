package com.dwell.domain.community.dto.response;

import com.dwell.domain.community.enums.Board;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Schema(title = "PostDetailResponse DTO", description = "게시물 상세 (댓글 포함)")
public class PostDetailResponse {

    @Schema(description = "게시물 ID", example = "31")
    private Long postId;

    @Schema(description = "게시판 (FREE / TIPS)", example = "FREE")
    private Board board;

    @Schema(description = "제목", example = "Floor 5 — our leak came back after repair")
    private String title;

    @Schema(description = "내용", example = "Same line as yours? The plumber said it's the riser pipe between floors 5 and 6.")
    private String content;

    @Schema(description = "작성자")
    private PostAuthorResponse author;

    @Schema(description = "태그 (#포함)", example = "[\"#ceilingleak\", \"#floor5\"]")
    private List<String> tags;

    @Schema(description = "이미지 (등록 순서)")
    private List<PostImageResponse> images;

    @Schema(description = "좋아요 수", example = "8")
    private long likeCount;

    @Schema(description = "내 좋아요 여부", example = "false")
    private boolean likedByMe;

    @Schema(description = "작성 시각", example = "2026-10-03T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "댓글 (오래된 순)")
    private List<ReplyResponse> replies;
}
