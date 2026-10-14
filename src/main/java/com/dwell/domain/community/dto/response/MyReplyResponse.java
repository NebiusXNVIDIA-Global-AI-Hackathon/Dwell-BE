package com.dwell.domain.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(title = "MyReplyResponse DTO", description = "내 댓글 카드 (누르면 게시물 상세로 이동)")
public class MyReplyResponse {

    @Schema(description = "댓글 ID", example = "77")
    private Long replyId;

    @Schema(description = "게시물 ID (상세 조회용)", example = "31")
    private Long postId;

    @Schema(description = "게시물 제목 (On: 게시물 제목)", example = "Floor 5 — our leak came back after repair")
    private String postTitle;

    @Schema(description = "댓글 내용", example = "Same here. Ours started after the repair too.")
    private String content;

    @Schema(description = "작성 시각 (1d, 2d는 클라에서 계산)", example = "2026-10-06T10:00:00")
    private LocalDateTime createdAt;
}
