package com.dwell.domain.community.dto.response;

import com.dwell.domain.community.enums.Board;
import com.dwell.global.common.enums.FloorBand;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Schema(title = "PostSummaryResponse DTO", description = "게시물 목록 카드")
public class PostSummaryResponse {

    @Schema(description = "게시물 ID", example = "31")
    private Long postId;

    @Schema(description = "게시판 (FREE / TIPS)", example = "FREE")
    private Board board;

    @Schema(description = "Tip 배지 여부 (board == TIPS)", example = "false")
    private Boolean isTip; // boolean이면 Lombok getter가 isTip()이라 JSON 키가 "tip"이 됨

    @Schema(description = "작성자 층 구간 (LOWER / MIDDLE / UPPER)", example = "MIDDLE")
    private FloorBand floorBand;

    @Schema(description = "제목", example = "Floor 5 — our leak came back after repair")
    private String title;

    @Schema(description = "내용 미리보기 (최대 100자, 넘으면 …)", example = "Same line as yours? The plumber said it's the riser pipe…")
    private String preview;

    @Schema(description = "태그 (#포함)", example = "[\"#ceilingleak\", \"#floor5\"]")
    private List<String> tags;

    @Schema(description = "첫 번째 이미지 URL (10분 동안 유효한 presigned URL, 없으면 null)", example = "https://...")
    private String thumbnailUrl;

    @Schema(description = "좋아요 수", example = "8")
    private long likeCount;

    @Schema(description = "댓글 수", example = "6")
    private long replyCount;

    @Schema(description = "작성 시각", example = "2026-10-03T10:00:00")
    private LocalDateTime createdAt;
}
